package org.nicolie.towersforpgm.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.Draft;

public class AddCommand implements CommandExecutor {
    private final TowersForPGM plugin;
    private final Draft draft;
    public AddCommand(TowersForPGM plugin, Draft draft) {
        this.plugin = plugin;
        this.draft = draft;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPluginMessage("errors.noPlayer"));
            return true;
        }
        if(!draft.isDraftActive()){
            sender.sendMessage(plugin.getPluginMessage("picks.noDraft"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getPluginMessage("add.usage"));
            return true;
        }
        if (args[0].equalsIgnoreCase(draft.getCaptain1Name()) || args[0].equalsIgnoreCase(draft.getCaptain2Name())) {
            sender.sendMessage(plugin.getPluginMessage("add.captain"));
            return true;
        }
        String playerName = args[0];
        draft.addToDraft(playerName);
        return true;
    }
}