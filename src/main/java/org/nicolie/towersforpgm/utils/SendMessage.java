package org.nicolie.towersforpgm.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
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

    public static void soundToWorld(String worldName, String sound, float volume, float pitch) {
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            System.out.println("El mundo '" + worldName + "' no existe o no está cargado.");
            return;
        }

        for (Player player : world.getPlayers()) {
            player.playSound(player.getLocation(), sound, 1.0f, 2.0f);
        }
    }

    public static void soundBroadcast(String sound, float volume, float pitch) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    public static void titleBroadcast(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (title == null || subtitle == null) {
            return;
        }
        String coloredTitle = ChatColor.translateAlternateColorCodes('&', title);
        String coloredSubtitle = ChatColor.translateAlternateColorCodes('&', subtitle);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(coloredTitle, coloredSubtitle, fadeIn, stay, fadeOut);
        }
    }
}