package org.nicolie.towersforpgm.matchbot.commands.stats;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.events.interaction.command.*;
import net.dv8tion.jda.api.events.interaction.component.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.*;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.commands.AutocompleteHandler;
import org.nicolie.towersforpgm.matchbot.embeds.StatsEmbed;
import org.nicolie.towersforpgm.utils.EloChartGenerator;
import org.nicolie.towersforpgm.utils.LanguageManager;

public class StatsCommand extends ListenerAdapter {

  public static final String NAME = "stats";

  private static final Map<String, CachedStats> CACHE = new ConcurrentHashMap<>();
  private static final long CACHE_TTL = 60_000;

  public static void register() {
    JDA jda = DiscordBot.getJDA();
    if (jda != null) {
      jda.addEventListener(new StatsCommand());

      var command =
          jda.upsertCommand(NAME, LanguageManager.message("matchbot.cmd.stats.description"));

      command.addOption(
          OptionType.STRING,
          LanguageManager.message("matchbot.cmd.stats.player"),
          LanguageManager.message("matchbot.cmd.stats.desc-player"),
          true,
          true);

      if (AutocompleteHandler.shouldUseAutocompleteForTables()) {
        command.addOption(
            OptionType.STRING,
            LanguageManager.message("matchbot.cmd.stats.table"),
            LanguageManager.message("matchbot.cmd.stats.desc-table"),
            false,
            true);
      } else {
        var tableOption = new net.dv8tion.jda.api.interactions.commands.build.OptionData(
            OptionType.STRING,
            LanguageManager.message("matchbot.cmd.stats.table"),
            LanguageManager.message("matchbot.cmd.stats.desc-table"),
            false);
        tableOption.addChoices(AutocompleteHandler.getTableChoices());
        command.addOptions(tableOption);
      }

      command.addOption(
          OptionType.BOOLEAN,
          LanguageManager.message("matchbot.cmd.stats.ephemeral"),
          LanguageManager.message("matchbot.cmd.stats.ephemeral-desc"),
          false);

      command.queue();
    }
  }

  @Override
  public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
    if (!event.getName().equals(NAME)) return;

    try {
      OptionMapping playerOpt =
          event.getOption(LanguageManager.message("matchbot.cmd.stats.player"));

      OptionMapping tableOpt = event.getOption(LanguageManager.message("matchbot.cmd.stats.table"));

      OptionMapping ephemeralOpt =
          event.getOption(LanguageManager.message("matchbot.cmd.stats.ephemeral"));

      if (playerOpt == null) {
        event.reply("❌ Error: falta el jugador").setEphemeral(true).queue();
        return;
      }

      String player = playerOpt.getAsString();
      boolean ephemeral = ephemeralOpt != null && ephemeralOpt.getAsBoolean();

      List<String> tables = MatchBotConfig.getTables();
      String table =
          tableOpt != null ? tableOpt.getAsString() : (tables.isEmpty() ? null : tables.get(0));

      if (table == null || !tables.contains(table)) {
        event.reply("❌ Tabla inválida").setEphemeral(true).queue();
        return;
      }

      event.deferReply(ephemeral).queue(hook -> {
        StatsManager.getStats(table, player).whenComplete((stats, throwable) -> {
          if (throwable != null) {
            hook.editOriginal("❌ `" + player + "` no se encuentra en `" + table + "`")
                .queue();
            return;
          }

          StatsManager.getEloHistory(player, table).whenComplete((eloHistory, err) -> {
            File chartFile = null;

            if (eloHistory != null && !eloHistory.isEmpty()) {
              String path = "charts/" + player + "_" + UUID.randomUUID() + ".png";
              chartFile = EloChartGenerator.generateEloChart(eloHistory, player, path);
            }

            String cacheId = UUID.randomUUID().toString();
            CACHE.put(cacheId, new CachedStats(table, stats, eloHistory, chartFile));

            EmbedBuilder embed = StatsEmbed.createNormal(table, player, stats);

            if (chartFile != null && chartFile.exists()) {
              embed.setImage("attachment://elo.png");
            }

            var buttons = List.of(Button.primary("stats:pergame:" + cacheId, "📈 Por partida"));

            if (chartFile != null && chartFile.exists()) {
              hook.editOriginalEmbeds(embed.build())
                  .setAttachments(FileUpload.fromData(chartFile, "elo.png"))
                  .setActionRow(buttons)
                  .queue();
            } else {
              hook.editOriginalEmbeds(embed.build()).setActionRow(buttons).queue();
            }
          });
        });
      });

    } catch (Exception e) {
      e.printStackTrace();
      event.reply("❌ Error interno").setEphemeral(true).queue();
    }
  }

  @Override
  public void onButtonInteraction(ButtonInteractionEvent event) {
    String id = event.getComponentId();

    if (!id.startsWith("stats:")) return;

    String[] parts = id.split(":");
    if (parts.length < 3) return;

    String mode = parts[1];
    String cacheId = parts[2];

    CachedStats cached = CACHE.get(cacheId);

    if (cached == null || cached.isExpired(CACHE_TTL)) {
      event.reply("⏱ Expirado, usa el comando otra vez").setEphemeral(true).queue();
      return;
    }
    cached.refresh();
    event.deferEdit().queue();

    boolean isNormal = mode.equals("normal");

    EmbedBuilder embed = isNormal
        ? StatsEmbed.createNormal(
            cached.getTable(), cached.getStats().getUsername(), cached.getStats())
        : StatsEmbed.createPerGame(
            cached.getTable(), cached.getStats().getUsername(), cached.getStats());

    if (cached.getChartFile() != null && cached.getChartFile().exists()) {
      embed.setImage("attachment://elo.png");
    }

    List<Button> buttons;

    if (isNormal) {
      buttons = List.of(Button.primary("stats:pergame:" + cacheId, "📈 Por partida"));
    } else {
      buttons = List.of(Button.primary("stats:normal:" + cacheId, "📊 Normal"));
    }

    if (cached.getChartFile() != null && cached.getChartFile().exists()) {
      event
          .getHook()
          .editOriginalEmbeds(embed.build())
          .setAttachments(FileUpload.fromData(cached.getChartFile(), "elo.png"))
          .setActionRow(buttons)
          .queue();
    } else {
      event.getHook().editOriginalEmbeds(embed.build()).setActionRow(buttons).queue();
    }
  }
}
