package org.nicolie.towersforpgm.commands.ranked;

import java.util.ArrayList;
import java.util.List;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.embeds.RankedNotify;
import org.nicolie.towersforpgm.rankeds.Queue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;

public class TagCommand {
  private static long lastExecution = 0;
  private static final long COOLDOWN_MILLIS = 30 * 60 * 1000;

  @Command("tag")
  @CommandDescription("Tag players in Discord to start rankeds")
  public void tagCommand(Player sender) {
    if (!TowersForPGM.getInstance().isMatchBotEnabled()) {
      return;
    }

    Match match = PGM.get().getMatchManager().getMatch(sender);
    MatchPlayer player = match.getPlayer(sender);

    // TODO: Si voice chat está activado: Verificar que tenga el rango en discord y esté en voice
    // chat queue para tagear

    if (player.isParticipating()) {
      player.sendWarning(Component.translatable("ranked.matchbot.tagNotAvailable"));
      return;
    }

    int onlinePlayers = match.getPlayers().size();
    int minSize = TowersForPGM.getInstance().config().ranked().getRankedMinSize();
    if (onlinePlayers >= minSize) {
      player.sendWarning(Queue.RANKED_PREFIX
          .append(Component.space())
          .append(Component.translatable("ranked.matchbot.tagNotNeeded")));
      return;
    }

    long now = System.currentTimeMillis();
    if (now - lastExecution < COOLDOWN_MILLIS) {
      long remaining = (COOLDOWN_MILLIS - (now - lastExecution)) / 1000;
      long min = remaining / 60;
      long sec = remaining % 60;
      String time = String.format("%d:%02d", min, sec);
      player.sendWarning(Component.translatable("command.cooldown", Component.text(time)));
      return;
    }

    lastExecution = now;
    sendEmbed(sender, match, player);
  }

  private void sendEmbed(Player sender, Match match, MatchPlayer player) {
    List<String> usernames = new ArrayList<>();
    for (MatchPlayer matchPlayer : match.getPlayers()) {
      // TODO: Si voice chat está activado: Usar lista de usuarios en vc en vez de todos los
      // jugadores del match
      usernames.add(matchPlayer.getNameLegacy());
    }

    StatsManager.getEloForUsernames(
            TowersForPGM.getInstance().config().databaseTables().getRankedDefaultTable(), usernames)
        .thenAccept(eloChanges -> {
          EmbedBuilder embed = RankedNotify.create(sender, match, eloChanges);
          DiscordBot.sendMatchEmbed(
              embed,
              MatchBotConfig.getDiscordChannel(),
              MatchBotConfig.getRankedRoleId(),
              DiscordBot.setEmbedThumbnail(match.getMap(), embed));

          Bukkit.getScheduler()
              .runTask(
                  TowersForPGM.getInstance(),
                  () -> match.sendMessage(Queue.RANKED_PREFIX
                      .append(Component.space())
                      .append(
                          Component.translatable("ranked.matchbot.tagSent", player.getName()))));
        })
        .exceptionally(throwable -> null);
  }
}
