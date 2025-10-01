package org.nicolie.towersforpgm.matchbot.commands.stats;

import java.util.List;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.commands.AutocompleteHandler;
import org.nicolie.towersforpgm.matchbot.embeds.StatsEmbed;
import org.nicolie.towersforpgm.utils.LanguageManager;

public class StatsCommand extends ListenerAdapter {
  private static final LanguageManager lang = TowersForPGM.getInstance().getLanguageManager();
  public static final String NAME = "stats";

  public static void register() {
    JDA jda = DiscordBot.getJDA();
    if (jda != null) {
      jda.addEventListener(new StatsCommand());

      var command = jda.upsertCommand(NAME, lang.getPluginMessage("matchbot.stats.description"))
          .addOption(
              net.dv8tion.jda.api.interactions.commands.OptionType.STRING,
              lang.getPluginMessage("matchbot.stats.player"),
              lang.getPluginMessage("matchbot.stats.desc-player"),
              true,
              true); // Siempre autocompletado para jugadores

      // Determinar si usar autocompletado o choices fijos para tablas
      if (AutocompleteHandler.shouldUseAutocompleteForTables()) {
        command.addOption(
            net.dv8tion.jda.api.interactions.commands.OptionType.STRING,
            lang.getPluginMessage("matchbot.stats.table"),
            lang.getPluginMessage("matchbot.stats.desc-table"),
            false,
            true); // Autocompletado
      } else {
        var tableOption = new net.dv8tion.jda.api.interactions.commands.build.OptionData(
            net.dv8tion.jda.api.interactions.commands.OptionType.STRING,
            lang.getPluginMessage("matchbot.stats.table"),
            lang.getPluginMessage("matchbot.stats.desc-table"),
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

    OptionMapping playerOpt = event.getOption(lang.getPluginMessage("matchbot.stats.player"));
    OptionMapping tableOpt = event.getOption(lang.getPluginMessage("matchbot.stats.table"));

    String player = playerOpt.getAsString();
    List<String> tables = MatchBotConfig.getTables();
    String table =
        tableOpt != null ? tableOpt.getAsString() : (tables.isEmpty() ? null : tables.get(0));

    if (tables != null && !tables.contains(table)) {
      event.reply(lang.getPluginMessage("matchbot.stats.invalid-table").replace("{table}", table)).setEphemeral(true).queue();
      return;
    }

    event.deferReply().queue(hook -> {
      StatsManager.getStats(table, player).whenComplete((stats, throwable) -> {
        if (throwable != null) {
          hook.editOriginalEmbeds(StatsEmbed.createError(
                      table, player, lang.getPluginMessage("matchbot.stats.error"))
                  .build())
              .queue();
          return;
        }
        hook.editOriginalEmbeds(StatsEmbed.create(table, player, stats).build()).queue();
      });
    });
  }
}
