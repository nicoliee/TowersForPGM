package org.nicolie.towersforpgm.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class SendMessage {
  public static void sendToAdmins(String message) {
    String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);

    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.hasPermission("towers.admin")) {
        player.sendMessage(coloredMessage);
      }
    }
  }

  public static void sendToDevelopers(Match match, Component message) {
    if (match == null) {
      return;
    }

    for (Player online : Bukkit.getOnlinePlayers()) {
      MatchPlayer target = PGM.get().getMatchManager().getPlayer(online);
      if (target == null || target.getMatch() == null || !target.getMatch().equals(match)) {
        continue;
      }

      if (online.hasPermission(Permissions.DEVELOPER)) {
        target.sendMessage(message);
      }
    }
  }

  public static String formatTime(long timeElapsed) {
    long minutes = timeElapsed / 60;
    long seconds = timeElapsed % 60;

    return String.format("%d:%02d", minutes, seconds);
  }
}
