package org.nicolie.towersforpgm.utils;

import org.nicolie.towersforpgm.TowersForPGM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private static List<String> cachedTables;
    private static String cachedSendToTable;
    private static Map<String, String> cachedMapTables = new HashMap<>();
    private static Map<String, Boolean> cachedMapPrivateMatch = new HashMap<>();
    private static boolean isVaultEnabled = false;
    private static int killReward = 0;
    private static int winReward = 0;

    public static void loadConfig() {
        TowersForPGM plugin = TowersForPGM.getInstance();
        plugin.reloadConfig();

        // Cargar y almacenar en memoria
        cachedTables = plugin.getConfig().getStringList("database.tables");
        cachedSendToTable = plugin.getConfig().getString("database.sendToTable");

        // Cargar mapas y sus tablas
        cachedMapTables.clear();
        if (plugin.getConfig().contains("stats.maps")) {
            for (String map : plugin.getConfig().getConfigurationSection("stats.maps").getKeys(false)) {
                cachedMapTables.put(map, plugin.getConfig().getString("stats.maps." + map + ".sendToTable"));
                cachedMapPrivateMatch.put(map, plugin.getConfig().getBoolean("stats.maps." + map + ".privateMatch", false));
            }
        }
        // Verificar si Vault está habilitado
        isVaultEnabled = plugin.getConfig().getBoolean("vault.enabled", false);
        killReward = plugin.getConfig().getInt("vault.killReward", 0);
        winReward = plugin.getConfig().getInt("vault.winReward", 0);


    }

    public static List<String> getTables() {
        return cachedTables;
    }

    public static String getSendToTable() {
        return cachedSendToTable;
    }

    public static Map<String, String> getMapTables() {
        return cachedMapTables;
    }

    public static String getTableForMap(String mapName) {
        return cachedMapTables.getOrDefault(mapName, cachedSendToTable);
    }

    public static Map<String, String> getTableForMaps() {
        Map<String, String> mapTables = new HashMap<>(cachedMapTables);
        mapTables.put("default", cachedSendToTable);
        return mapTables;
    }

    public static void setSendToTable(String tableName) {
        TowersForPGM plugin = TowersForPGM.getInstance();
        cachedSendToTable = tableName;
        plugin.getConfig().set("database.sendToTable", tableName);
        plugin.saveConfig();
    }

    public static void addMapTable(String mapName, String tableName) {
        TowersForPGM plugin = TowersForPGM.getInstance();
        cachedMapTables.put(mapName, tableName);
        plugin.getConfig().set("stats.maps." + mapName + ".sendToTable", tableName);
        plugin.saveConfig();
    }

    public static void removeMapTable(String mapName) {
        TowersForPGM plugin = TowersForPGM.getInstance();
        if (cachedMapTables.containsKey(mapName)) {
            cachedMapTables.remove(mapName);
            plugin.getConfig().set("stats.maps." + mapName + ".sendToTable", null);
            plugin.saveConfig();
        }
    }

    public static void addTable(String tableName) {
        TowersForPGM plugin = TowersForPGM.getInstance();

        // Clonar la lista para asegurarnos de que es modificable
        cachedTables = new ArrayList<>(cachedTables);

        if (!cachedTables.contains(tableName)) {
            cachedTables.add(tableName);
            plugin.getConfig().set("database.tables", cachedTables);
            plugin.saveConfig();
        }
    }

    public static void removeTable(String tableName) {
        TowersForPGM plugin = TowersForPGM.getInstance();

        // Clonar la lista para asegurarnos de que es modificable
        cachedTables = new ArrayList<>(cachedTables);

        if (cachedTables.contains(tableName)) {
            cachedTables.remove(tableName);
            plugin.getConfig().set("database.tables", cachedTables);
            plugin.saveConfig();
        }
    }

    public static boolean isPrivateMatch(String mapName) {
        return cachedMapPrivateMatch.getOrDefault(mapName, false);
    }

    public static void setPrivateMatch(String mapName, boolean privateMatch) {
        TowersForPGM plugin = TowersForPGM.getInstance();
        cachedMapPrivateMatch.put(mapName, privateMatch);
        plugin.getConfig().set("stats.maps." + mapName + ".privateMatch", privateMatch);
        plugin.saveConfig();
    }

    public static boolean isVaultEnabled() {
        return isVaultEnabled;
    }

    public static void setVaultEnabled(boolean enabled) {
        TowersForPGM plugin = TowersForPGM.getInstance();
        isVaultEnabled = enabled;
        plugin.getConfig().set("vault.enabled", enabled);
        plugin.saveConfig();
    }

    public static int getKillReward() {
        return killReward;
    }

    public static void setKillReward(int reward) {
        TowersForPGM plugin = TowersForPGM.getInstance();
        killReward = reward;
        plugin.getConfig().set("vault.killReward", reward);
        plugin.saveConfig();
    }

    public static int getWinReward() {
        return winReward;
    }

    public static void setWinReward(int reward) {
        TowersForPGM plugin = TowersForPGM.getInstance();
        winReward = reward;
        plugin.getConfig().set("vault.winReward", reward);
        plugin.saveConfig();
    }
}