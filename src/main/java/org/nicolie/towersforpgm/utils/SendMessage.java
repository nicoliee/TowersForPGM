package org.nicolie.towersforpgm.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SendMessage {
  public static void sendToConsole(String message) {
    if (message == null) {
      return;
    }
    String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
    Bukkit.getConsoleSender().sendMessage(coloredMessage);
  }

  public static void broadcast(String message) {
    String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
    if (message == null) {
      return;
    }
    Bukkit.broadcastMessage(coloredMessage);
  }

  public static void sendToPlayer(Player player, String message) {
    if (player == null) {
      return;
    }
    if (message == null) {
      return;
    }
    String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
    player.sendMessage(coloredMessage);
  }

  public static void sendToPlayer(CommandSender sender, String message) {
    if (sender == null) {
      return;
    }
    if (message == null) {
      return;
    }
    String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
    sender.sendMessage(coloredMessage);
  }

  public static void sendToWorld(String worldName, String message) {
    World world = Bukkit.getWorld(worldName);

    if (world == null) {
      System.out.println("El mundo '" + worldName + "' no existe o no está cargado.");
      return;
    }

    String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);

    for (Player player : world.getPlayers()) {
      player.sendMessage(coloredMessage);
    }
  }

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
