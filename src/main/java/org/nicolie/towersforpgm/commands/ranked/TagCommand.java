package org.nicolie.towersforpgm.commands.ranked;

import java.util.ArrayList;
import java.util.List;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.embeds.RankedNotify;
import org.nicolie.towersforpgm.rankeds.Queue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.Audience;

public class TagCommand implements CommandExecutor {
  private static long lastExecution = 0;
  private static final long COOLDOWN_MILLIS = 30 * 60 * 1000;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Audience audience = Audience.get(sender);
    if (!TowersForPGM.getInstance().isMatchBotEnabled()) {
      return true;
    }
    if (!(sender instanceof Player)) {
      audience.sendWarning(Component.translatable("command.onlyPlayers"));
      return true;
    }
    Match match = PGM.get().getMatchManager().getMatch(sender);
    String map = match.getMap().getName();
    MatchPlayer player = match.getPlayer((Player) sender);
    boolean isRankedMap = TowersForPGM.getInstance().config().ranked().isMapRanked(map);
    if (!isRankedMap) {
      audience.sendWarning(Queue.RANKED_PREFIX
          .append(Component.space())
          .append(Component.translatable("ranked.notRankedMap"), Component.text(map)));
      return true;
    }
    if (player.isParticipating()) {
      player.sendWarning(Component.translatable("ranked.matchbot.tagNotAvailable"));
      return true;
    }

    // Solo permitir tag si no hay suficientes jugadores online para el mínimo
    int onlinePlayers = match.getPlayers().size();
    int minSize = TowersForPGM.getInstance().config().ranked().getRankedMinSize();
    if (onlinePlayers >= minSize) {
      player.sendWarning(Queue.RANKED_PREFIX
          .append(Component.space())
          .append(Component.translatable("ranked.matchbot.tagNotNeeded")));
      return true;
    }
    long now = System.currentTimeMillis();
    if (now - lastExecution < COOLDOWN_MILLIS) {
      long remaining = (COOLDOWN_MILLIS - (now - lastExecution)) / 1000;
      long min = remaining / 60;
      long sec = remaining % 60;
      String time = String.format("%d:%02d", min, sec);
      player.sendWarning(Component.translatable("command.cooldown", Component.text(time)));
      return true;
    }
    lastExecution = now;
    sendEmbed(sender, match, player);
    return true;
  }

  private void sendEmbed(CommandSender sender, Match match, MatchPlayer player) {
    List<String> usernames = new ArrayList<>();
    for (MatchPlayer matchPlayer : match.getPlayers()) {
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
          org.bukkit.Bukkit.getScheduler()
              .runTask(
                  TowersForPGM.getInstance(),
                  () -> match.sendMessage(Queue.RANKED_PREFIX
                      .append(Component.space())
                      .append(
                          Component.translatable("ranked.matchbot.tagSent", player.getName()))));
        })
        .exceptionally(throwable -> {
          return null;
        });
  }
}
