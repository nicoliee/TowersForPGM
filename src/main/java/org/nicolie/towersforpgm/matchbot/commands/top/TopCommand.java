package org.nicolie.towersforpgm.matchbot.commands.top;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.database.TopResult;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.cache.TopLRUCache;
import org.nicolie.towersforpgm.matchbot.commands.AutocompleteHandler;
import org.nicolie.towersforpgm.matchbot.embeds.TopEmbed;
import org.nicolie.towersforpgm.matchbot.enums.Stat;
import org.nicolie.towersforpgm.utils.LanguageManager;

public class TopCommand extends ListenerAdapter {
  private static final LanguageManager lang = TowersForPGM.getInstance().getLanguageManager();
  public static final String NAME = "top";
  private static final String DESC = lang.getPluginMessage("matchbot.top.description");
  private static final int PAGE_SIZE = 10;
  private static final String OPT_STAT = lang.getPluginMessage("matchbot.top.stat");
  private static final String DESC_STAT = lang.getPluginMessage("matchbot.top.desc-stat");
  private static final String OPT_TABLE = lang.getPluginMessage("matchbot.top.table");
  private static final String DESC_TABLE = lang.getPluginMessage("matchbot.top.desc-table");
  private static final String OPT_PER_GAME = lang.getPluginMessage("matchbot.top.per-game");
  private static final String DESC_PER_GAME = lang.getPluginMessage("matchbot.top.desc-per-game");

  public static void register() {
    JDA jda = DiscordBot.getJDA();
    if (jda != null) {
      jda.addEventListener(new TopCommand());

      CommandCreateAction command = jda.upsertCommand(NAME, DESC);

      // Determinar si usar autocompletado o choices fijos para estadísticas
      if (AutocompleteHandler.shouldUseAutocompleteForStats()) {
        command.addOption(
            net.dv8tion.jda.api.interactions.commands.OptionType.STRING,
            OPT_STAT,
            DESC_STAT,
            true,
            true); // Autocompletado
      } else {
        var statOption = new net.dv8tion.jda.api.interactions.commands.build.OptionData(
            net.dv8tion.jda.api.interactions.commands.OptionType.STRING, OPT_STAT, DESC_STAT, true);
        statOption.addChoices(AutocompleteHandler.getStatChoices());
        command.addOptions(statOption);
      }

      // Determinar si usar autocompletado o choices fijos para tablas
      if (AutocompleteHandler.shouldUseAutocompleteForTables()) {
        command.addOption(
            net.dv8tion.jda.api.interactions.commands.OptionType.STRING,
            OPT_TABLE,
            DESC_TABLE,
            false,
            true); // Autocompletado
      } else {
        var tableOption = new net.dv8tion.jda.api.interactions.commands.build.OptionData(
            net.dv8tion.jda.api.interactions.commands.OptionType.STRING,
            OPT_TABLE,
            DESC_TABLE,
            false);
        tableOption.addChoices(AutocompleteHandler.getTableChoices());
        command.addOptions(tableOption);
      }

      command
          .addOption(
              net.dv8tion.jda.api.interactions.commands.OptionType.BOOLEAN,
              OPT_PER_GAME,
              DESC_PER_GAME,
              false,
              false)
          .queue();
    }
  }

  @Override
  public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
    if (!event.getName().equals(NAME)) return;

    OptionMapping statOpt = event.getOption(OPT_STAT);
    OptionMapping tableOpt = event.getOption(OPT_TABLE);
    OptionMapping perGameOpt = event.getOption(OPT_PER_GAME);

    if (statOpt == null) {
      return;
    }

    String statName = statOpt.getAsString();
    List<String> tables = MatchBotConfig.getTables();
    String table =
        tableOpt != null ? tableOpt.getAsString() : (tables.isEmpty() ? null : tables.get(0));
    int page = 1;
    Stat stat = Stat.fromDisplayName(statName);

    boolean perGame = perGameOpt != null && perGameOpt.getAsBoolean();

    // Validar que la estadística soporte perGame si el usuario lo solicitó
    if (perGame && !stat.isStatPerGame()) {
      event
          .reply(lang.getPluginMessage("matchbot.top.perGame-restriction")
              .replace("{stat}", stat.getDisplayName()))
          .setEphemeral(true)
          .queue();
      return;
    }

    event
        .deferReply()
        .queue(
            hook -> CompletableFuture.runAsync(() -> {
              try {
                String dbColumn = stat.getDbColumn();
                boolean isDamage = stat == Stat.DAMAGE_DONE || stat == Stat.DAMAGE_TAKEN;
                if (perGame || isDamage) {
                  dbColumn = dbColumn + "PerGame";
                }
                // Intentar recuperar de caché (solo página 1 por simplicidad)
                TopLRUCache.CachedPage cached = null;
                if (page == 1) {
                  cached = TopLRUCache.get(table, dbColumn, page, PAGE_SIZE, perGame);
                }

                TopResult result;
                if (cached != null) {
                  result = new TopResult(cached.data(), cached.totalRecords());
                } else {
                  // Usar el nuevo método combinado que obtiene datos y total en una sola consulta
                  result = StatsManager.getTop(table, dbColumn, PAGE_SIZE, page).get();
                  if (page == 1 && !result.getData().isEmpty()) {
                    double lastValue =
                        result.getData().get(result.getData().size() - 1).getValue();
                    String lastUser =
                        result.getData().get(result.getData().size() - 1).getUsername();
                    TopLRUCache.put(
                        table,
                        dbColumn,
                        page,
                        PAGE_SIZE,
                        perGame,
                        result.getData(),
                        result.getTotalRecords(),
                        lastValue,
                        lastUser);
                  }
                }

                if (result.getData().isEmpty()) {
                  hook.sendMessage(lang.getPluginMessage("matchbot.top.invalid-column")
                          .replace("{stat}", stat.getDisplayName())
                          .replace("{table}", table))
                      .setEphemeral(true)
                      .queue();
                  return;
                }
                EmbedBuilder embed = TopEmbed.createTopEmbed(stat, table, 1, result, perGame);

                String dbColumnFinal = dbColumn;
                String token = org.nicolie.towersforpgm.matchbot.commands.top.TopPaginationListener
                    .createStateToken(
                        table,
                        stat,
                        perGame,
                        dbColumnFinal,
                        result.getTotalRecords(),
                        result.getData());

                hook.sendMessageEmbeds(embed.build())
                    .addActionRow(
                        Button.secondary("top_prev_" + token, "⬅️").asDisabled(),
                        Button.secondary("top_next_" + token, "➡️")
                            .withDisabled(
                                result.getData().isEmpty() || result.getData().size() < PAGE_SIZE))
                    .queue();
              } catch (Exception e) {
                hook.sendMessage(lang.getPluginMessage("matchbot.top.error")
                        .replace("{error}", e.getMessage()))
                    .queue();
              }
            }));
  }
}
