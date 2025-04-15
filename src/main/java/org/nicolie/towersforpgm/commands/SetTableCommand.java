package org.nicolie.towersforpgm.commands;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;

import tc.oc.pgm.api.PGM;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class SetTableCommand implements CommandExecutor, TabCompleter {
    private final LanguageManager languageManager;

    public SetTableCommand(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }
    

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(languageManager.getPluginMessage("setTable.usage"));
            return true;
        }
        String action = args[0];
        String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName(); // Obtener el mapa actual

        switch (action.toLowerCase()) {
            case "sendtotable":
                if (args.length < 2) {
                    sender.sendMessage(languageManager.getPluginMessage("setTable.usageSendToTable"));
                    return true;
                }
                String tableName = args[1];
                if (!ConfigManager.getTables().contains(tableName)) {
                    sender.sendMessage(languageManager.getPluginMessage("setTable.notExists")
                            .replace("{table}", tableName));
                    return true;
                }
                ConfigManager.setSendToTable(tableName);
                sender.sendMessage(languageManager.getPluginMessage("setTable.success")
                        .replace("{table}", tableName));
                break;

            case "addmap":
                if (args.length < 2) {
                    sender.sendMessage(languageManager.getPluginMessage("setTable.usageAddMap"));
                    return true;
                }
                String tableForMap = args[1];
                if (!ConfigManager.getTables().contains(tableForMap)) {
                    sender.sendMessage(languageManager.getPluginMessage("setTable.notExists")
                            .replace("{table}", tableForMap));
                    return true;
                }
                ConfigManager.addMapTable(mapName, tableForMap);
                sender.sendMessage(languageManager.getPluginMessage("setTable.mapAdded")
                        .replace("{map}", mapName)
                        .replace("{table}", tableForMap));
                break;

            case "deletemap":
                if (!ConfigManager.getMapTables().containsKey(mapName)) {
                    sender.sendMessage(languageManager.getPluginMessage("setTable.mapNotExists")
                            .replace("{map}", mapName));
                    return true;
                }
                ConfigManager.removeMapTable(mapName);
                sender.sendMessage(languageManager.getPluginMessage("setTable.mapDeleted")
                        .replace("{map}", mapName));
                break;

            default:
                sender.sendMessage(languageManager.getPluginMessage("setTable.usage"));
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("sendToTable", "addMap", "deleteMap")
                    .stream()
                    .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("sendToTable")) {
            return ConfigManager.getTables().stream()
                    .filter(table -> table.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("addMap")) {
            return ConfigManager.getTables().stream()
                    .filter(table -> table.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}