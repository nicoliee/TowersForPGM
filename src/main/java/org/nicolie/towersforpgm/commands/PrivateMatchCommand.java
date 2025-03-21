package org.nicolie.towersforpgm.commands;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.SendMessage;

public class PrivateMatchCommand implements CommandExecutor, TabCompleter{
    private final TowersForPGM plugin;
    public PrivateMatchCommand(TowersForPGM plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPluginMessage("errors.noPlayer"));
            return true;
        }
        Player player = (Player) sender;
        if (args.length != 1) {
            SendMessage.sendToPlayer(player, plugin.getPluginMessage("privateMatch.usage"));
            return true;
        }
        String action = args[0];
        String mapName = plugin.getCurrentMap(); // Obtener el mapa actual
        switch (action.toLowerCase()) {
            case "true":
                ConfigManager.setPrivateMatch(mapName, true);
                SendMessage.sendToPlayer(player, plugin.getPluginMessage("privateMatch.true")
                        .replace("{map}", mapName));
                break;
            case "false":
                ConfigManager.setPrivateMatch(mapName, false);
                SendMessage.sendToPlayer(player, plugin.getPluginMessage("privateMatch.false")
                        .replace("{map}", mapName));
                break;
            default:
                SendMessage.sendToPlayer(player, plugin.getPluginMessage("privateMatch.usage"));
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("true", "false");
        }
        return null;
    }
}
