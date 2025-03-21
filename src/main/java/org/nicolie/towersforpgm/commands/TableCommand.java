package org.nicolie.towersforpgm.commands;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.TableManager;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.SendMessage;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TableCommand implements CommandExecutor, TabCompleter {
    private final TowersForPGM plugin;
    public TableCommand(TowersForPGM plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPluginMessage("errors.notPlayer"));
            return true;
        }
        
        if (args.length < 1) {
            sender.sendMessage(plugin.getPluginMessage("table.usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getPluginMessage("table.specify"));
                    return true;
                }
                if (ConfigManager.getTables().contains(args[1])) {
                    SendMessage.sendToPlayer(player, plugin.getPluginMessage("table.exists"));
                    return true;
                }
                String addTable = args[1];
                ConfigManager.addTable(addTable);
                TableManager.createTable(addTable);
                SendMessage.sendToPlayer(player, plugin.getPluginMessage("table.created"));
                break;

            case "delete":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getPluginMessage("table.specify"));
                    return true;
                }
                String delTable = args[1];
                if (!ConfigManager.getTables().contains(delTable)) {
                    SendMessage.sendToPlayer(player, plugin.getPluginMessage("table.notFound"));
                    return true;
                }
                ConfigManager.removeTable(delTable);
                SendMessage.sendToPlayer(player, plugin.getPluginMessage("table.deleted"));
                break;

            case "list":
                List<String> tables = ConfigManager.getTables();
                SendMessage.sendToPlayer(player, plugin.getPluginMessage("table.list"));
                SendMessage.sendToPlayer(player, " &e"+String.join(", ", tables));
                break;

            default:
                SendMessage.sendToPlayer(player,plugin.getPluginMessage("table.usage"));
        }
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Lista de opciones posibles
            List<String> options = Arrays.asList("add", "delete", "list");
            // Filtrar las opciones que comienzan con el texto ingresado por el usuario
            String input = args[0].toLowerCase();
            return options.stream()
                    .filter(option -> option.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }
        if (args[0].equalsIgnoreCase("delete") && args.length == 2) {
            List<String> tables = ConfigManager.getTables();
            String input = args[1].toLowerCase();
            return tables.stream()
                    .filter(table -> table.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }
        return null;
    }
}