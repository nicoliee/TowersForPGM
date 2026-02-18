package org.nicolie.towersforpgm.matchbot.commands.stats;

import java.io.File;
import java.util.List;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.commands.AutocompleteHandler;
import org.nicolie.towersforpgm.matchbot.embeds.StatsEmbed;
import org.nicolie.towersforpgm.utils.EloChartGenerator;
import org.nicolie.towersforpgm.utils.LanguageManager;

public class StatsCommand extends ListenerAdapter {
  public static final String NAME = "stats";

  public static void register() {
    JDA jda = DiscordBot.getJDA();
    if (jda != null) {
      jda.addEventListener(new StatsCommand());

      var command = jda.upsertCommand(
              NAME, LanguageManager.message("matchbot.cmd.stats.description"))
          .addOption(
              net.dv8tion.jda.api.interactions.commands.OptionType.STRING,
              LanguageManager.message("matchbot.cmd.stats.player"),
              LanguageManager.message("matchbot.cmd.stats.desc-player"),
              true,
              true);

      if (AutocompleteHandler.shouldUseAutocompleteForTables()) {
        command.addOption(
            net.dv8tion.jda.api.interactions.commands.OptionType.STRING,
            LanguageManager.message("matchbot.cmd.stats.table"),
            LanguageManager.message("matchbot.cmd.stats.desc-table"),
            false,
            true);
      } else {
        var tableOption = new net.dv8tion.jda.api.interactions.commands.build.OptionData(
            net.dv8tion.jda.api.interactions.commands.OptionType.STRING,
            LanguageManager.message("matchbot.cmd.stats.table"),
            LanguageManager.message("matchbot.cmd.stats.desc-table"),
            false);
        tableOption.addChoices(AutocompleteHandler.getTableChoices());
        command.addOptions(tableOption);
      }

      command.queue();
    }
  }

  @Override
  public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
    if (!event.getName().equals(NAME)) return;

    OptionMapping playerOpt = event.getOption(LanguageManager.message("matchbot.cmd.stats.player"));
    OptionMapping tableOpt = event.getOption(LanguageManager.message("matchbot.cmd.stats.table"));

    String player = playerOpt.getAsString();
    List<String> tables = MatchBotConfig.getTables();
    String table =
        tableOpt != null ? tableOpt.getAsString() : (tables.isEmpty() ? null : tables.get(0));

    if (tables != null && !tables.contains(table)) {
      event
          .reply(
              LanguageManager.message("matchbot.cmd.stats.invalid-table").replace("{table}", table))
          .setEphemeral(true)
          .queue();
      return;
    }

    event.deferReply().queue(hook -> {
      StatsManager.getStats(table, player).whenComplete((stats, throwable) -> {
        if (throwable != null) {
          hook.editOriginalEmbeds(StatsEmbed.createError(
                      table, player, LanguageManager.message("matchbot.cmd.stats.error"))
                  .build())
              .queue();
          return;
        }

        // Crear el embed de stats
        var statsEmbed = StatsEmbed.create(table, player, stats);

        // Obtener el historial de ELO
        StatsManager.getEloHistory(player, table).whenComplete((eloHistory, eloThrowable) -> {
          if (eloThrowable != null || eloHistory == null || eloHistory.isEmpty()) {
            // Si hay error o no hay historial, enviar solo el embed de stats
            hook.editOriginalEmbeds(statsEmbed.build()).queue();
            return;
          }

          // Generar la gráfica
          String chartPath = "charts/" + table + "/" + player + "_elo.png";
          File chartFile = EloChartGenerator.generateEloChart(eloHistory, player, chartPath);

          if (chartFile != null && chartFile.exists()) {
            // Agregar la imagen al embed
            statsEmbed.setImage("attachment://elo_chart.png");
            hook.editOriginalEmbeds(statsEmbed.build())
                .setFiles(FileUpload.fromData(chartFile, "elo_chart.png"))
                .queue(
                    success -> {
                      // Eliminar el archivo temporal después de enviarlo
                      chartFile.delete();
                    },
                    error -> {
                      // Si falla, intentar enviar sin imagen
                      statsEmbed.setImage(null);
                      hook.editOriginalEmbeds(statsEmbed.build()).queue();
                      chartFile.delete();
                    });
          } else {
            // Si no se pudo generar la gráfica, enviar solo el embed de stats
            hook.editOriginalEmbeds(statsEmbed.build()).queue();
          }
        });
      });
    });
  }
}
