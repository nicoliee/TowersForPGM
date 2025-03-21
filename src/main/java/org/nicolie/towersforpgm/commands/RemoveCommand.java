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
import org.nicolie.towersforpgm.draft.Draft;

public class RemoveCommand implements CommandExecutor, TabCompleter{
    private final TowersForPGM plugin;
    private final Draft draft;
    public RemoveCommand(TowersForPGM plugin, Draft draft) {
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
            sender.sendMessage(plugin.getPluginMessage("remove.usage"));
            return true;
        }
        if (args[0].equalsIgnoreCase(draft.getCaptain1Name()) || args[0].equalsIgnoreCase(draft.getCaptain2Name())) {
            sender.sendMessage(plugin.getPluginMessage("remove.captain"));
            return true;
        }
        String playerName = args[0];
        draft.removeFromDraft(playerName);
        return true;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Obtener la lista de tablas desde TowersForPGM.getTables()
            List<String> tables = draft.getAvailablePlayers().stream().map(Player::getName).collect(Collectors.toList());
            tables.addAll(draft.getAvailableOfflinePlayers());
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
}
