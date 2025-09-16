package org.nicolie.towersforpgm.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.nicolie.towersforpgm.TowersForPGM;

public class ConfigManager {
  // --- Configuración de la base de datos ---
  private static List<String> Tables;
  private static String defaultTable;
  private static String tempTable;
  private static Map<String, String> MapTables = new HashMap<>();
  private static Map<String, Boolean> MapPrivateMatch = new HashMap<>();

  // --- Configuración del draft ---
  private static boolean draftSuggestions;
  private static boolean draftTimer;
  private static boolean secondPickBalance;
  private static String order;
  private static int minOrder;

  // --- Configuración de rankeds ---
  private static int rankedSize;
  private static String rankedOrder;
  private static List<String> rankedTables;
  private static String rankedDefaultTable;
  private static List<String> rankedMaps;
  private static boolean rankedMatchmaking;

  // --- MatchBot para rankeds ---
  private static String rankedChannel;
  private static String rankedRoleID;

  public static void loadConfig() {
    TowersForPGM plugin = TowersForPGM.getInstance();
    plugin.reloadConfig();

    // Cargar y almacenar en memoria
    Tables = plugin.getConfig().getStringList("database.tables");
    defaultTable = plugin.getConfig().getString("database.defaultTable");

    // Cargar mapas y sus tablas
    MapTables.clear();
    if (plugin.getConfig().contains("stats.maps")) {
      for (String map : plugin.getConfig().getConfigurationSection("stats.maps").getKeys(false)) {
        MapTables.put(map, plugin.getConfig().getString("stats.maps." + map + ".table"));
        MapPrivateMatch.put(
            map, plugin.getConfig().getBoolean("stats.maps." + map + ".privateMatch", false));
      }
    }

    // Cargar configuraciones del draft
    draftSuggestions = plugin.getConfig().getBoolean("draft.suggestions", false);
    draftTimer = plugin.getConfig().getBoolean("draft.timer", false);
    secondPickBalance = plugin.getConfig().getBoolean("draft.secondPickBalance", false);
    order = plugin.getConfig().getString("draft.order", "");
    minOrder = plugin.getConfig().getInt("draft.minOrder", 0);

    // Cargar configuraciones de rankeds
    rankedSize = plugin.getConfig().getInt("rankeds.size", 0);
    rankedOrder = plugin.getConfig().getString("rankeds.order", "");
    rankedTables = plugin.getConfig().getStringList("rankeds.tables");
    rankedDefaultTable = plugin.getConfig().getString("rankeds.defaultTable", "");
    rankedMaps = plugin.getConfig().getStringList("rankeds.maps");
    rankedMatchmaking = plugin.getConfig().getBoolean("rankeds.matchmaking", false);
    // Cargar configuraciones de matchbot para rankeds
    rankedChannel = plugin.getConfig().getString("rankeds.matchbot.discordChannel", "");
    rankedRoleID = plugin.getConfig().getString("rankeds.matchbot.rankedRoleId", "");
  }

  // --- Tablas ---

  // Obtener la lista combinada de tablas (normales y ranked)
  public static List<String> getTables() {
    List<String> combinedTables = new ArrayList<>();
    if (Tables != null) {
      combinedTables.addAll(Tables);
    }
    if (rankedTables != null) {
      for (String table : rankedTables) {
        if (!combinedTables.contains(table)) {
          combinedTables.add(table);
        }
      }
    }
    return combinedTables;
  }

  // Agregar una tabla a la lista de tablas
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

  // Eliminar una tabla de la lista de tablas
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

  // Establecer la tabla por defecto
  public static void setDefaultTable(String tableName) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    defaultTable = tableName;
    plugin.getConfig().set("database.defaultTable", tableName);
    plugin.saveConfig();
  }

  // --- Mapas y sus tablas ---

  // Obtener el mapa de tablas
  public static Map<String, String> getMapTables() {
    return MapTables;
  }

  // Obtener la tabla asociada a un mapa
  public static String getTableForMap(String mapName) {
    return MapTables.getOrDefault(mapName, defaultTable);
  }

  // Agregar una tabla a un mapa
  public static void addMapTable(String mapName, String tableName) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    MapTables.put(mapName, tableName);
    plugin.getConfig().set("stats.maps." + mapName + ".table", tableName);
    plugin.saveConfig();
  }

  // Eliminar una tabla de un mapa
  public static void removeMapTable(String mapName) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (MapTables.containsKey(mapName)) {
      MapTables.remove(mapName);
      plugin.getConfig().set("stats.maps." + mapName + ".table", null);
      plugin.saveConfig();
    }
  }

  public static void addTempTable(String tableName) {
    tempTable = tableName;
  }

  public static void removeTempTable() {
    tempTable = null;
  }

  public static String getTempTable() {
    return tempTable;
  }

  // Verificar si un mapa tiene partida privada habilitada
  public static boolean isPrivateMatch(String mapName) {
    return MapPrivateMatch.getOrDefault(mapName, false);
  }

  // Establecer el estado de partida privada para un mapa
  public static void setPrivateMatch(String mapName, boolean privateMatch) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    MapPrivateMatch.put(mapName, privateMatch);
    plugin.getConfig().set("stats.maps." + mapName + ".privateMatch", privateMatch);
    plugin.saveConfig();
  }

  // --- Configuraciones del draft ---

  // Verificar si las sugerencias de draft están habilitadas
  public static boolean isDraftSuggestions() {
    return draftSuggestions;
  }

  // Establecer el estado de las sugerencias de draft
  public static void setDraftSuggestions(boolean draftSuggestions) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    ConfigManager.draftSuggestions = draftSuggestions;
    plugin.getConfig().set("draft.suggestions", draftSuggestions);
    plugin.saveConfig();
  }

  // Verificar si el temporizador de draft está habilitado
  public static boolean isDraftTimer() {
    return draftTimer;
  }

  // Establecer el estado del temporizador de draft
  public static void setDraftTimer(boolean draftTimer) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    ConfigManager.draftTimer = draftTimer;
    plugin.getConfig().set("draft.timer", draftTimer);
    plugin.saveConfig();
  }

  // Verificar si el segundo jugador obtiene un jugador extra en el draft
  public static boolean isSecondPickBalance() {
    return secondPickBalance;
  }

  // Establecer el estado de si el segundo jugador obtiene un jugador extra en el draft
  public static void setSecondPickBalance(boolean secondPickBalance) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    ConfigManager.secondPickBalance = secondPickBalance;
    plugin.getConfig().set("draft.secondPickBalance", secondPickBalance);
    plugin.saveConfig();
  }

  // Obtener el orden del draft
  public static String getDraftOrder() {
    return order;
  }

  // Establecer el orden del draft
  public static void setDraftOrder(String order) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    ConfigManager.order = order;
    plugin.getConfig().set("draft.order", order);
    plugin.saveConfig();
  }

  // Obtener el mínimo de orden del draft
  public static int getMinDraftOrder() {
    return minOrder;
  }

  // Establecer el mínimo de orden del draft
  public static void setMinDraftOrder(int minOrder) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    ConfigManager.minOrder = minOrder;
    plugin.getConfig().set("draft.minOrder", minOrder);
    plugin.saveConfig();
  }

  // --- Configuraciones de rankeds ---
  public static int getRankedSize() {
    return rankedSize;
  }

  public static void setRankedSize(int rankedSize) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    ConfigManager.rankedSize = rankedSize;
    plugin.getConfig().set("rankeds.size", rankedSize);
    plugin.saveConfig();
  }

  public static String getRankedOrder() {
    return rankedOrder;
  }

  public static void setRankedOrder(String rankedOrder) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    ConfigManager.rankedOrder = rankedOrder;
    plugin.getConfig().set("rankeds.order", rankedOrder);
    plugin.saveConfig();
  }

  public static List<String> getRankedTables() {
    return rankedTables;
  }

  public static void addRankedTable(String tableName) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    rankedTables.add(tableName);
    plugin.getConfig().set("rankeds.tables", rankedTables);
    plugin.saveConfig();
  }

  public static void removeRankedTable(String tableName) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    rankedTables.remove(tableName);
    plugin.getConfig().set("rankeds.tables", rankedTables);
    plugin.saveConfig();
  }

  public static String getRankedDefaultTable() {
    return rankedDefaultTable;
  }

  public static void setRankedDefaultTable(String rankedDefaultTable) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    ConfigManager.rankedDefaultTable = rankedDefaultTable;
    plugin.getConfig().set("rankeds.defaultTable", rankedDefaultTable);
    plugin.saveConfig();
  }

  public static List<String> getRankedMaps() {
    return rankedMaps;
  }

  public static void addRankedMap(String mapName) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    rankedMaps.add(mapName);
    plugin.getConfig().set("rankeds.maps", rankedMaps);
    plugin.saveConfig();
  }

  public static void removeRankedMap(String mapName) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    rankedMaps.remove(mapName);
    plugin.getConfig().set("rankeds.maps", rankedMaps);
    plugin.saveConfig();
  }

  public static boolean isRankedMatchmaking() {
    return rankedMatchmaking;
  }

  public static void setRankedMatchmaking(boolean rankedMatchmaking) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    ConfigManager.rankedMatchmaking = rankedMatchmaking;
    plugin.getConfig().set("rankeds.matchmaking", rankedMatchmaking);
    plugin.saveConfig();
  }

  // --- MatchBot ---
  public static String getRankedChannel() {
    return rankedChannel;
  }

  public static String getRankedRoleID() {
    return rankedRoleID;
  }
}
