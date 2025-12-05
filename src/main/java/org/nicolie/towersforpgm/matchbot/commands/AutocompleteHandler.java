package org.nicolie.towersforpgm.matchbot.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.enums.Stat;
import org.nicolie.towersforpgm.utils.LanguageManager;

public class AutocompleteHandler extends ListenerAdapter {
  private static final Map<String, List<Command.Choice>> CHOICES_CACHE = new ConcurrentHashMap<>();
  private static final List<Command.Choice> EMPTY_CHOICES = List.of();
  private static final int MAX_CHOICES_WITHOUT_AUTOCOMPLETE = 25;

  public static boolean shouldUseAutocompleteForStats() {
    return getAvailableStatNames().size() > MAX_CHOICES_WITHOUT_AUTOCOMPLETE;
  }

  public static boolean shouldUseAutocompleteForTables() {
    return MatchBotConfig.getTables().size() > MAX_CHOICES_WITHOUT_AUTOCOMPLETE;
  }

  public static List<Command.Choice> getStatChoices() {
    return getAvailableStatNames().stream()
        .map(name -> new Command.Choice(name, name))
        .collect(Collectors.toUnmodifiableList());
  }

  public static List<Command.Choice> getTableChoices() {
    return MatchBotConfig.getTables().stream()
        .map(name -> new Command.Choice(name, name))
        .collect(Collectors.toUnmodifiableList());
  }

  public static void register() {
    initializeChoices();
    JDA jda = DiscordBot.getJDA();
    if (jda != null) {
      jda.addEventListener(new AutocompleteHandler());
    }
  }

  private static void initializeChoices() {
    if (shouldUseAutocompleteForStats()) {
      List<Command.Choice> statChoices = getAvailableStatNames().stream()
          .map(name -> new Command.Choice(name, name))
          .collect(Collectors.toUnmodifiableList());
      CHOICES_CACHE.put(LanguageManager.message("matchbot.top.stat"), statChoices);
    }

    if (shouldUseAutocompleteForTables()) {
      List<Command.Choice> tableChoices = MatchBotConfig.getTables().stream()
          .map(name -> new Command.Choice(name, name))
          .collect(Collectors.toUnmodifiableList());
      CHOICES_CACHE.put(LanguageManager.message("matchbot.top.table"), tableChoices);
    }
  }

  private static List<String> getAvailableStatNames() {
    return Arrays.stream(Stat.values())
        .filter(stat -> {
          // Si la estadística Points está deshabilitada en config, filtrarla
          if (stat == Stat.POINTS && !MatchBotConfig.isStatsPointsEnabled()) {
            return false;
          }
          return true;
        })
        .map(Stat::getDisplayName)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
    String focused = event.getFocusedOption().getName();
    String userInput = event.getFocusedOption().getValue();

    if ("history".equals(event.getName())
        && LanguageManager.message("matchbot.history.matchid").equals(focused)) {
      org.nicolie.towersforpgm.database.MatchHistoryManager.getRecentMatchIds(
              userInput == null ? "" : userInput)
          .thenAccept(matchIds -> {
            List<Command.Choice> choices = matchIds.stream()
                .limit(25)
                .map(id -> new Command.Choice(id, id))
                .collect(Collectors.toList());
            event.replyChoices(choices).queue();
          })
          .exceptionally(err -> {
            event.replyChoices(EMPTY_CHOICES).queue();
            return null;
          });
      return;
    }

    if (LanguageManager.message("matchbot.stats.player").equals(focused)) {
      StatsManager.getAllUsernamesFiltered(userInput == null ? "" : userInput)
          .thenAccept(players -> {
            List<Command.Choice> choices = players.stream()
                .limit(25)
                .map(p -> new Command.Choice(p, p))
                .collect(Collectors.toList());
            event.replyChoices(choices).queue();
          })
          .exceptionally(err -> {
            event.replyChoices(EMPTY_CHOICES).queue();
            return null;
          });
      return;
    }

    List<Command.Choice> choices = filterChoices(event.getName(), focused, userInput);
    event.replyChoices(choices).queue();
  }

  private List<Command.Choice> filterChoices(
      String commandName, String optionName, String userInput) {
    // Note: async autocompletions (history and stats player) are handled in
    // onCommandAutoCompleteInteraction since they return CompletableFutures.
    // This method only handles cached/static choices.
    List<Command.Choice> cachedChoices = CHOICES_CACHE.get(optionName);

    if (cachedChoices == null) {
      return EMPTY_CHOICES;
    }

    if (userInput == null || userInput.trim().isEmpty()) {
      return cachedChoices.size() <= 25 ? cachedChoices : cachedChoices.subList(0, 25);
    }

    String lowerInput = userInput.toLowerCase();
    return cachedChoices.stream()
        .filter(choice -> choice.getName().toLowerCase().startsWith(lowerInput))
        .limit(25)
        .collect(Collectors.toList());
  }

  public static void refreshCache() {
    CHOICES_CACHE.clear();
    initializeChoices();
  }
}
