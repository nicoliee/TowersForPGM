package org.nicolie.towersforpgm.commandUtils;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.TableManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.SendMessage;

import tc.oc.pgm.api.PGM;
public class StatsConfig {
    private final TowersForPGM plugin = TowersForPGM.getInstance();
    private final LanguageManager languageManager;

    public StatsConfig(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    public void toggleStats(CommandSender sender){
        boolean isStatsCancel = plugin.isStatsCancel();
        plugin.setStatsCancel(!isStatsCancel);
        String cancelStatsMessage = isStatsCancel ? "&aStats collection has been enabled."
                : "&cStats collection has been disabled.";
        SendMessage.sendToPlayer(sender, cancelStatsMessage);
    }

    public void setDefaultTable(CommandSender sender, String table){
        if (!ConfigManager.getTables().contains(table)) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("table.notFound")
                    .replace("{table}", table));
            return;
        }
        ConfigManager.setDefaultTable(table);
        SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("table.success")
                .replace("{table}", table));
    }

    public void addTable(CommandSender sender, String table){
        if (ConfigManager.getTables().contains(table)) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("table.exists"));
            return;
        }

        ConfigManager.addTable(table);
        TableManager.createTable(table);
        SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("table.created")
                .replace("{table}", table));
    }

    public void deleteTable(CommandSender sender, String table){
        if (!ConfigManager.getTables().contains(table)) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("table.notFound")
                    .replace("{table}", table));
            return;
        }
        ConfigManager.removeTable(table);
        SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("table.deleted")
                .replace("{table}", table));
    }

    public void listTables(CommandSender sender){
        List<String> tables = ConfigManager.getTables();
        if (tables.isEmpty()) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("table.noTables"));
            return;
        }
        String tablesList = String.join(", ", tables);
        SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("table.list")
                .replace("{list}", tablesList));
    }

    public void addTableForMap(CommandSender sender, String table){
        String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
        if (!ConfigManager.getTables().contains(table)) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("table.notFound")
                    .replace("{table}", table));
            return;
        }
        ConfigManager.addMapTable(mapName, table);
        SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("table.mapAdded")
                .replace("{map}", mapName)
                .replace("{table}", table));
    }

    public void deleteTableForMap(CommandSender sender){
        String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
        if (!ConfigManager.getMapTables().containsKey(mapName)) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("table.mapNotExists")
                    .replace("{map}", mapName));
            return;
        }
        ConfigManager.removeMapTable(mapName);
        SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("table.mapDeleted")
                .replace("{map}", mapName));
    }

    public void addTempTable(CommandSender sender, String table){
        ConfigManager.addTempTable(table);
        SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("table.tempAdded")
                .replace("{table}", table));
    }

    public void removeTempTable(CommandSender sender){
        if (ConfigManager.getTempTable() == null) {
            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("table.tempNotExists"));
            return;
        }
        ConfigManager.removeTempTable();
        SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("table.tempRemoved"));
    }
}