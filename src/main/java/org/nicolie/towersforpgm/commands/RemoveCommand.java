package org.nicolie.towersforpgm.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.Teams;
import org.nicolie.towersforpgm.utils.SendMessage;

public class RemoveCommand implements CommandExecutor, TabCompleter{
    private final TowersForPGM plugin;
    private final Draft draft;
    private final Teams teams;
    private final AvailablePlayers availablePlayers;
    public RemoveCommand(TowersForPGM plugin, Draft draft, Teams teams, Captains captains, AvailablePlayers availablePlayers) {
        this.plugin = plugin;
        this.draft = draft;
        this.teams = teams;
        this.availablePlayers = availablePlayers;
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
            sender.sendMessage(plugin.getPluginMessage("remove.usage"));
            return true;
        }
        String playerName = args[0];
        if (isInvalidPlayer(playerName, sender)) {
            return true;
        }
        availablePlayers.removePlayer(playerName);
        SendMessage.broadcast(plugin.getConfigurableMessage("picks.remove").replace("{player}", playerName));
        SendMessage.soundBroadcast("note.pling", 1f, 0.5f);
        return true;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Obtener la lista de tablas desde TowersForPGM.getTables()
            List<String> tables = availablePlayers.getAvailablePlayers().stream().map(Player::getName).collect(Collectors.toList());
            tables.addAll(availablePlayers.getAvailableOfflinePlayers());
            // Filtrar las opciones que comienzan con el texto ingresado por el usuario
            String input = args[0].toLowerCase();
            List<String> filteredOptions = new ArrayList<>();
            for (String table : tables) {
                if (table.toLowerCase().startsWith(input)) {
                    filteredOptions.add(table);
                }
            }
            return filteredOptions;
        }
        return null;
    }

    private boolean isInvalidPlayer(String playerName, CommandSender sender) {
        if (!availablePlayers.getAllAvailablePlayers().contains(playerName)) {
            sendErrorMessage(sender, "remove.notInDraft");
            return true;
        }

        if (teams.isPlayerInAnyTeam(playerName)) {
            sendErrorMessage(sender, "captains.alreadyInTeam");
            return true;
        }

        return false;
    }

    private void sendErrorMessage(CommandSender sender, String messageKey) {
        sender.sendMessage(plugin.getPluginMessage(messageKey));
    }
}