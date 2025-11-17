package org.nicolie.towersforpgm.matchbot.commands.history;

import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.nicolie.towersforpgm.database.MatchHistoryManager;
import org.nicolie.towersforpgm.matchbot.embeds.HistoryEmbed;
import org.nicolie.towersforpgm.utils.LanguageManager;

public class HistoryCommand extends ListenerAdapter {
  public static final String NAME = "history";

  public static void register() {
    JDA jda = DiscordBot.getJDA();
    if (jda != null) {
      jda.addEventListener(new HistoryCommand());

      var command = jda.upsertCommand(
              NAME, LanguageManager.langMessage("matchbot.history.description"))
          .addOption(
              net.dv8tion.jda.api.interactions.commands.OptionType.STRING,
              LanguageManager.langMessage("matchbot.history.matchid"),
              LanguageManager.langMessage("matchbot.history.desc-matchid"),
              true,
              true);

      command.queue();
    }
  }

  @Override
  public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
    if (!event.getName().equals(NAME)) return;

    OptionMapping matchIdOpt =
        event.getOption(LanguageManager.langMessage("matchbot.history.matchid"));
    String matchId = matchIdOpt.getAsString();

    event.deferReply().queue(hook -> {
      MatchHistoryManager.getMatch(matchId).whenComplete((history, throwable) -> {
        if (throwable != null) {
          hook.editOriginalEmbeds(HistoryEmbed.createError(
                      matchId, LanguageManager.langMessage("matchbot.history.error"))
                  .build())
              .queue();
          return;
        }
        if (history == null) {
          hook.editOriginalEmbeds(HistoryEmbed.createError(
                      matchId, LanguageManager.langMessage("matchbot.history.notfound"))
                  .build())
              .queue();
          return;
        }
        hook.editOriginalEmbeds(HistoryEmbed.create(history).build()).queue();
      });
    });
  }
}
