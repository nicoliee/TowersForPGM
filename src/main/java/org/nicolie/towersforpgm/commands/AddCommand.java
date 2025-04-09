package org.nicolie.towersforpgm.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.PickInventory;
import org.nicolie.towersforpgm.draft.Teams;
import org.nicolie.towersforpgm.utils.SendMessage;

public class AddCommand implements CommandExecutor {
    private final AvailablePlayers availablePlayers;
    private final Captains captains;
    private final Draft draft;
    private final Teams teams;
    private final TowersForPGM plugin;
    private final PickInventory pickInventory;

    public AddCommand(AvailablePlayers availablePlayers, Captains captains, Draft draft, Teams teams, TowersForPGM plugin, PickInventory pickInventory) {
        this.availablePlayers = availablePlayers;
        this.captains = captains;
        this.draft = draft;
        this.teams = teams;
        this.plugin = plugin;
        this.pickInventory = pickInventory;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sendErrorMessage(sender, "errors.noPlayer");
            return true;
        }

        if (!draft.isDraftActive()) {
            sendErrorMessage(sender, "picks.noDraft");
            return true;
        }

        if (args.length < 1) {
            sendErrorMessage(sender, "add.usage");
            return true;
        }

        String playerName = args[0];

        if (isInvalidPlayer(playerName, sender)) {
            return true;
        }

        availablePlayers.addPlayer(playerName);
        pickInventory.updateAllInventories();
        SendMessage.broadcast(plugin.getConfigurableMessage("picks.add").replace("{player}", playerName));
        SendMessage.soundBroadcast("note.pling", 1f, 2f);
        return true;
    }

    private boolean isInvalidPlayer(String playerName, CommandSender sender) {
        if ((captains.getCaptain1Name() != null && captains.getCaptain1Name().equalsIgnoreCase(playerName)) ||
            (captains.getCaptain2Name() != null && captains.getCaptain2Name().equalsIgnoreCase(playerName))) {
            sendErrorMessage(sender, "add.captain");
            return true;
        }


        if (teams.isPlayerInAnyTeam(playerName)) {
            sendErrorMessage(sender, "captains.alreadyInTeam");
            return true;
        }

        if (availablePlayers.getAllAvailablePlayers().contains(playerName)) {
            sendErrorMessage(sender, "captains.alreadyInDraft");
            return true;
        }
        return false;
    }

    private void sendErrorMessage(CommandSender sender, String messageKey) {
        sender.sendMessage(plugin.getPluginMessage(messageKey));
    }
}