package org.nicolie.towersforpgm.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.database.models.top.Top;
import org.nicolie.towersforpgm.rankeds.Rank;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.text.TextFormatter;

public class EloCommand implements CommandExecutor {
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(LanguageManager.langMessage("ranked.prefix")
          + LanguageManager.langMessage("errors.noPlayer"));
      return true;
    }
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer((Player) sender);
    Match match = matchPlayer.getMatch();
    String map = match.getMap().getName();
    boolean isRankedMap = false;
    if (ConfigManager.getRankedMaps() != null && map != null) {
      for (String ranked : ConfigManager.getRankedMaps()) {
        if (ranked.equalsIgnoreCase(map)) {
          isRankedMap = true;
          break;
        }
      }
    }
    boolean matchInProgress = match != null && match.isRunning() && matchPlayer.isParticipating();

    if (!isRankedMap || matchInProgress) {
      matchPlayer.sendWarning(Component.text(LanguageManager.langMessage("ranked.prefix")
          + LanguageManager.langMessage("system.notAvailable")));
      return true;
    }
    String table = ConfigManager.getRankedDefaultTable();
    if (args.length > 0 && "lb".equalsIgnoreCase(args[0])) {
      int page = 1;
      if (args.length > 1) {
        try {
          page = Math.max(1, Integer.parseInt(args[1]));
        } catch (NumberFormatException ignored) {
          page = 1;
        }
      }
      sendEloTop(sender, matchPlayer, table, page);
    } else {
      sendEloMessage(sender, matchPlayer, table, args);
    }
    return true;
  }

  private void sendEloMessage(
      CommandSender sender, MatchPlayer matchPlayer, String table, String[] args) {
    String targetName = args.length > 0 ? args[0] : matchPlayer.getNameLegacy();
    StatsManager.getStats(table, targetName).thenAccept(stats -> {
      if (stats == null) {
        // No hay estadísticas en la base de datos para ese jugador
        matchPlayer.sendWarning(Component.text(LanguageManager.langMessage("stats.noStats")));
        return;
      }

      Rank rank = Rank.getRankByElo(stats.getElo());
      Rank maxRank = Rank.getRankByElo(stats.getMaxElo());
      org.bukkit.entity.Player bukkitPlayerHeading =
          org.bukkit.Bukkit.getPlayerExact(stats.getUsername());
      String headingName = bukkitPlayerHeading != null
          ? PGM.get().getMatchManager().getPlayer(bukkitPlayerHeading).getPrefixedName()
          : "§3" + stats.getUsername();
      String eloMessage = rank.getPrefixedRank(true) + " " + headingName + "§8: " + rank.getColor()
          + stats.getElo() + " §8[" + maxRank.getColor() + stats.getMaxElo() + "§8]";

      matchPlayer.sendMessage(Component.text(eloMessage));
    });
  }

  private void sendEloTop(CommandSender sender, MatchPlayer matchPlayer, String table, int page) {
    final int PAGE_SIZE = 5;
    StatsManager.getTop(table, "elo", PAGE_SIZE, page).thenAccept(topResult -> {
      if (topResult == null
          || topResult.getData() == null
          || topResult.getData().isEmpty()) {
        matchPlayer.sendWarning(Component.text(LanguageManager.langMessage("ranked.prefix")
            + LanguageManager.langMessage("stats.noPage")));
        return;
      }
      matchPlayer.sendMessage(TextFormatter.horizontalLineHeading(
          sender,
          Component.text("Top " + LanguageManager.langMessage("stats.elo") + " §8[§f" + page
              + "§6/§f" + topResult.getTotalPages(PAGE_SIZE) + "§8]"),
          TextColor.color(0xFFFFFF),
          TextFormatter.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH * 3));

      int startIndex = (page - 1) * PAGE_SIZE;
      int index = 0;
      for (Top entry : topResult.getData()) {
        Rank rank = Rank.getRankByElo(entry.getValueAsInt());
        org.bukkit.entity.Player bukkitPlayer =
            org.bukkit.Bukkit.getPlayerExact(entry.getUsername());
        String nameDisplay = bukkitPlayer != null
            ? PGM.get().getMatchManager().getPlayer(bukkitPlayer).getPrefixedName()
            : "§3" + entry.getUsername();
        String user = rank.getPrefixedRank(true) + " " + nameDisplay;
        int position = startIndex + (++index);
        String line = String.format(
            "%d. %s§8: " + rank.getColor() + "%d", position, user, entry.getValueAsInt());
        matchPlayer.sendMessage(Component.text(line));
      }
      matchPlayer.sendMessage(TextFormatter.horizontalLine(
          TextColor.color(0xFFFFFF), TextFormatter.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH * 3));
    });
  }
}
