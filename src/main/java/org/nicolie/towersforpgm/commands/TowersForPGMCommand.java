package org.nicolie.towersforpgm.commands;

import java.io.File;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.SendMessage;

public class TowersForPGMCommand implements CommandExecutor, TabCompleter {
    private final TowersForPGM plugin;

    public TowersForPGMCommand(TowersForPGM plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPluginMessage("errors.noPlayer"));
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            SendMessage.sendToPlayer(player, "§8[§bTowersForPGM§8] §7Version: " + plugin.getDescription().getVersion());
            return true;
        }

        String argument = args[0].toLowerCase();

        switch (argument) {
            case "setlanguage":
                if (args.length < 2) {
                    SendMessage.sendToPlayer(player, plugin.getPluginMessage("TowersForPGM.noLanguage"));
                    return true;
                }
                String language = args[1].toLowerCase();
                if (!language.equals("en") && !language.equals("es")) {
                    SendMessage.sendToPlayer(player, plugin.getPluginMessage("TowersForPGM.invalidLanguage"));
                    return true;
                }
                plugin.setLanguage(language);
                SendMessage.sendToPlayer(player, plugin.getPluginMessage("TowersForPGM.languageSet"));
                return true;

            case "reloadmessages":
                File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
                SendMessage.sendToPlayer(player, plugin.getPluginMessage("messages.reloadStart"));

                if (messagesFile.exists()) {
                    if (!messagesFile.delete()) {
                        SendMessage.sendToPlayer(player, plugin.getPluginMessage("messages.reloadError"));
                        return true;
                    }
                }

                plugin.saveResource("messages.yml", false);
                plugin.saveDefaultMessages();
                SendMessage.sendToPlayer(player, plugin.getPluginMessage("messages.reloadSuccess"));
                return true;

            default:
                SendMessage.sendToPlayer(player, "§8[§bTowersForPGM§8] §7Versión: " + plugin.getDescription().getVersion());
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("towers.admin")) {
            return List.of(); // No sugerir nada si no tiene permisos
        }

        if (args.length == 1) {
            return List.of("setLanguage", "reloadMessages");
        }
        if (args[0].equalsIgnoreCase("setLanguage") && args.length == 2) {
            return List.of("en", "es");
        }
        return List.of();
    }
}
