package org.nicolie.towersforpgm.utils;

import org.nicolie.towersforpgm.TowersForPGM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private static List<String> Tables;
    private static String SendToTable;
    private static Map<String, String> MapTables = new HashMap<>();
    private static Map<String, Boolean> MapPrivateMatch = new HashMap<>();
    private static boolean draftSuggestions;
    private static boolean draftTimer;

    public static void loadConfig() {
        TowersForPGM plugin = TowersForPGM.getInstance();
        plugin.reloadConfig();

        // Cargar y almacenar en memoria
        Tables = plugin.getConfig().getStringList("database.tables");
        SendToTable = plugin.getConfig().getString("database.sendToTable");

        // Cargar mapas y sus tablas
        MapTables.clear();
        if (plugin.getConfig().contains("stats.maps")) {
            for (String map : plugin.getConfig().getConfigurationSection("stats.maps").getKeys(false)) {
                MapTables.put(map, plugin.getConfig().getString("stats.maps." + map + ".sendToTable"));
                MapPrivateMatch.put(map, plugin.getConfig().getBoolean("stats.maps." + map + ".privateMatch", false));
            }
        }

        // Cargar configuraciones del draft
        draftSuggestions = plugin.getConfig().getBoolean("draft.suggestions", false);
        draftTimer = plugin.getConfig().getBoolean("draft.timer", false);
    }

    public static List<String> getTables() {
        return Tables;
    }

    public static String getSendToTable() {
        return SendToTable;
    }

    public static Map<String, String> getMapTables() {
        return MapTables;
    }

    public static String getTableForMap(String mapName) {
        return MapTables.getOrDefault(mapName, SendToTable);
    }

    public static Map<String, String> getTableForMaps() {
        Map<String, String> mapTables = new HashMap<>(MapTables);
        mapTables.put("default", SendToTable);
        return mapTables;
    }

    public static void setSendToTable(String tableName) {
        TowersForPGM plugin = TowersForPGM.getInstance();
        SendToTable = tableName;
        plugin.getConfig().set("database.sendToTable", tableName);
        plugin.saveConfig();
    }

    public static void addMapTable(String mapName, String tableName) {
        TowersForPGM plugin = TowersForPGM.getInstance();
        MapTables.put(mapName, tableName);
        plugin.getConfig().set("stats.maps." + mapName + ".sendToTable", tableName);
        plugin.saveConfig();
    }

    public static void removeMapTable(String mapName) {
        TowersForPGM plugin = TowersForPGM.getInstance();
        if (MapTables.containsKey(mapName)) {
            MapTables.remove(mapName);
            plugin.getConfig().set("stats.maps." + mapName + ".sendToTable", null);
            plugin.saveConfig();
        }
    }

    public static void addTable(String tableName) {
        TowersForPGM plugin = TowersForPGM.getInstance();

        // Clonar la lista para asegurarnos de que es modificable
        Tables = new ArrayList<>(Tables);

        if (!Tables.contains(tableName)) {
            Tables.add(tableName);
            plugin.getConfig().set("database.tables", Tables);
            plugin.saveConfig();
        }
    }

    public static void removeTable(String tableName) {
        TowersForPGM plugin = TowersForPGM.getInstance();

        // Clonar la lista para asegurarnos de que es modificable
        Tables = new ArrayList<>(Tables);

        if (Tables.contains(tableName)) {
            Tables.remove(tableName);
            plugin.getConfig().set("database.tables", Tables);
            plugin.saveConfig();
        }
    }

    public static boolean isDraftSuggestions() {
        return draftSuggestions;
    }

    public static void setDraftSuggestions(boolean draftSuggestions) {
        TowersForPGM plugin = TowersForPGM.getInstance();
        ConfigManager.draftSuggestions = draftSuggestions;
        plugin.getConfig().set("draft.suggestions", draftSuggestions);
        plugin.saveConfig();
    }

    public static boolean isDraftTimer() {
        return draftTimer;
    }

    public static void setDraftTimer(boolean draftTimer) {
        TowersForPGM plugin = TowersForPGM.getInstance();
        ConfigManager.draftTimer = draftTimer;
        plugin.getConfig().set("draft.timer", draftTimer);
        plugin.saveConfig();
    }

    public static boolean isPrivateMatch(String mapName) {
        return MapPrivateMatch.getOrDefault(mapName, false);
    }

    public static void setPrivateMatch(String mapName, boolean privateMatch) {
        TowersForPGM plugin = TowersForPGM.getInstance();
        MapPrivateMatch.put(mapName, privateMatch);
        plugin.getConfig().set("stats.maps." + mapName + ".privateMatch", privateMatch);
        plugin.saveConfig();
    }
}