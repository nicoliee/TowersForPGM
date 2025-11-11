package org.nicolie.towersforpgm.matchbot.commands;

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
    return Stat.getAllStatNames().size() > MAX_CHOICES_WITHOUT_AUTOCOMPLETE;
  }

  public static boolean shouldUseAutocompleteForTables() {
    return MatchBotConfig.getTables().size() > MAX_CHOICES_WITHOUT_AUTOCOMPLETE;
  }

  public static List<Command.Choice> getStatChoices() {
    return Stat.getAllStatNames().stream()
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
      List<Command.Choice> statChoices = Stat.getAllStatNames().stream()
          .map(name -> new Command.Choice(name, name))
          .collect(Collectors.toUnmodifiableList());
      CHOICES_CACHE.put(LanguageManager.langMessage("matchbot.top.stat"), statChoices);
    }

    if (shouldUseAutocompleteForTables()) {
      List<Command.Choice> tableChoices = MatchBotConfig.getTables().stream()
          .map(name -> new Command.Choice(name, name))
          .collect(Collectors.toUnmodifiableList());
      CHOICES_CACHE.put(LanguageManager.langMessage("matchbot.top.table"), tableChoices);
    }
  }

  @Override
  public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
    String focused = event.getFocusedOption().getName();
    String userInput = event.getFocusedOption().getValue();

    List<Command.Choice> choices = filterChoices(event.getName(), focused, userInput);
    event.replyChoices(choices).queue();
  }

  private List<Command.Choice> filterChoices(
      String commandName, String optionName, String userInput) {
    // Autocomplete para /history matchid
    if ("history".equals(commandName)
        && LanguageManager.langMessage("matchbot.history.matchid").equals(optionName)) {
      List<String> matchIds =
          org.nicolie.towersforpgm.database.MatchHistoryManager.getRecentMatchIds(userInput);
      return matchIds.stream()
          .limit(25)
          .map(id -> new Command.Choice(id, id))
          .collect(Collectors.toList());
    }

    // Autocomplete para /stats player
    if (LanguageManager.langMessage("matchbot.stats.player").equals(optionName)) {
      List<String> players =
          StatsManager.getAllUsernamesFiltered(userInput == null ? "" : userInput);
      return players.stream()
          .limit(25)
          .map(p -> new Command.Choice(p, p))
          .collect(Collectors.toList());
    }

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
