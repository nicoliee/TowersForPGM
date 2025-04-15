package org.nicolie.towersforpgm.commands;

import java.util.Arrays;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

import tc.oc.pgm.api.PGM;

public class PrivateMatchCommand implements CommandExecutor, TabCompleter{
    private final LanguageManager languageManager;

    public PrivateMatchCommand(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getPluginMessage("errors.noPlayer"));
            return true;
        }
        Player player = (Player) sender;
        if (args.length != 1) {
            SendMessage.sendToPlayer(player, languageManager.getPluginMessage("privateMatch.usage"));
            return true;
        }
        String action = args[0];
        String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName(); // Obtener el mapa actual
        switch (action.toLowerCase()) {
            case "true":
                ConfigManager.setPrivateMatch(mapName, true);
                SendMessage.sendToPlayer(player, languageManager.getPluginMessage("privateMatch.true")
                        .replace("{map}", mapName));
                break;
            case "false":
                ConfigManager.setPrivateMatch(mapName, false);
                SendMessage.sendToPlayer(player, languageManager.getPluginMessage("privateMatch.false")
                        .replace("{map}", mapName));
                break;
            default:
                SendMessage.sendToPlayer(player, languageManager.getPluginMessage("privateMatch.usage"));
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("true", "false");
        }
        return null;
    }
}