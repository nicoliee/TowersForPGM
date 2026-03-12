package org.nicolie.towersforpgm.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class SendMessage {
  public static void sendToAdmins(String message) {
    String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);

    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.hasPermission("towers.admin")) {
        player.sendMessage(coloredMessage);
      }
    }
  }

  public static void sendToDevelopers(String message) {
    String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);

    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.hasPermission("towers.developer")) {
        player.sendMessage(coloredMessage);
      }
    }
  }

  public static String formatTime(long timeElapsed) {
    long minutes = timeElapsed / 60;
    long seconds = timeElapsed % 60;

    return String.format("%d:%02d", minutes, seconds);
  }
}
