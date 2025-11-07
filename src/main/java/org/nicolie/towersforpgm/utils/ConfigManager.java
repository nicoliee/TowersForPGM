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

  // --- Configuración de partidas privadas ---
  private static Map<String, Boolean> MapPrivateMatch = new HashMap<>();

  // --- Configuración del draft ---
  private static boolean draftSuggestions;
  private static boolean draftTimer;
  private static boolean secondPickBalance;
  private static String order;
  private static int minOrder;

  // --- Configuración de rankeds ---
  private static int disconnectTime;
  private static int rankedMinSize;
  private static int rankedMaxSize;
  private static String rankedOrder;
  private static List<String> rankedTables;
  private static String rankedDefaultTable;
  private static List<String> rankedMaps;
  private static boolean rankedMatchmaking;

  public static void loadConfig() {
    TowersForPGM plugin = TowersForPGM.getInstance();
    plugin.reloadConfig();
    MapTables.clear();
    MapPrivateMatch.clear();

    if (plugin.getConfig().contains("database.tables")
        && plugin.getConfig().getConfigurationSection("database.tables") != null) {
      Tables = new ArrayList<>(
          plugin.getConfig().getConfigurationSection("database.tables").getKeys(false));

      // Cargar mapas y sus tablas desde database.tables
      for (String table :
          plugin.getConfig().getConfigurationSection("database.tables").getKeys(false)) {
        List<String> maps = plugin.getConfig().getStringList("database.tables." + table);
        if (maps != null) {
          for (String map : maps) {
            if (map != null && !map.isEmpty()) {
              MapTables.put(map, table);
            }
          }
        }
      }
    } else {
      Tables = new ArrayList<>();
    }

    defaultTable = plugin.getConfig().getString("database.defaultTable");

    // Cargar mapas de privateMatch (lista dentro de database)
    if (plugin.getConfig().contains("privateMatch")) {
      List<String> privateMaps = plugin.getConfig().getStringList("privateMatch");
      if (privateMaps != null) {
        for (String map : privateMaps) {
          if (map != null && !map.isEmpty()) {
            MapPrivateMatch.put(map, true);
          }
        }
      }
    }

    // Cargar configuraciones del draft
    draftSuggestions = plugin.getConfig().getBoolean("draft.suggestions", false);
    draftTimer = plugin.getConfig().getBoolean("draft.timer", false);
    secondPickBalance = plugin.getConfig().getBoolean("draft.secondPickBalance", false);
    order = plugin.getConfig().getString("draft.order", "");
    minOrder = plugin.getConfig().getInt("draft.minOrder", 0);

    // Cargar configuraciones de rankeds
    disconnectTime = plugin.getConfig().getInt("rankeds.disconnectTime", 60);
    rankedMinSize = plugin.getConfig().getInt("rankeds.size.min", 4);
    rankedMaxSize = plugin.getConfig().getInt("rankeds.size.max", 8);
    rankedOrder = plugin.getConfig().getString("rankeds.order", "");
    rankedTables = plugin.getConfig().getStringList("rankeds.tables");
    rankedDefaultTable = plugin.getConfig().getString("rankeds.defaultTable", "");
    rankedMaps = plugin.getConfig().getStringList("rankeds.maps");
    rankedMatchmaking = plugin.getConfig().getBoolean("rankeds.matchmaking", false);
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
    if (Tables == null) {
      Tables = new ArrayList<>();
    }

    // Añadir la tabla en memoria y crear la sección database.tables.<tableName> como lista vacía
    if (!Tables.contains(tableName)) {
      Tables.add(tableName);
      plugin.getConfig().set("database.tables." + tableName, new ArrayList<String>());
      plugin.saveConfig();
    }
  }

  // Eliminar una tabla de la lista de tablas
  public static void removeTable(String tableName) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (Tables == null) {
      return;
    }

    if (Tables.contains(tableName)) {
      Tables.remove(tableName);
      // Eliminar la sección correspondiente en config
      plugin.getConfig().set("database.tables." + tableName, null);
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

  public static String getActiveTable(String mapName) {
    // Prioridad 1: tempTable
    if (tempTable != null && !tempTable.isEmpty()) {
      return tempTable;
    }

    // Prioridad 2: tabla específica del mapa
    if (mapName != null && MapTables.containsKey(mapName)) {
      String mapTable = MapTables.get(mapName);
      if (mapTable != null && !mapTable.isEmpty()) {
        return mapTable;
      }
    }

    // Prioridad 3: tabla por defecto
    return defaultTable != null ? defaultTable : "";
  }

  // Obtener el mapa de tablas
  public static Map<String, String> getMapTables() {
    return MapTables;
  }

  // Obtener la tabla asociada a un mapa
  public static String getTableForMap(String mapName) {
    return MapTables.getOrDefault(mapName, defaultTable);
  }

  // Agregar una tabla a un mapa
  public static void addMap(String mapName, String tableName) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    MapTables.put(mapName, tableName);

    // Añadir el mapa a la lista de la tabla en database.tables.<tableName>
    List<String> maps = plugin.getConfig().getStringList("database.tables." + tableName);
    if (maps == null) {
      maps = new ArrayList<>();
    }
    if (!maps.contains(mapName)) {
      maps.add(mapName);
      plugin.getConfig().set("database.tables." + tableName, maps);
      plugin.saveConfig();
    }
  }

  // Eliminar una tabla de un mapa
  public static void removeMap(String mapName) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (MapTables.containsKey(mapName)) {
      String tableName = MapTables.get(mapName);
      MapTables.remove(mapName);

      // Eliminar el mapa de la lista de la tabla en database.tables.<tableName>
      List<String> maps = plugin.getConfig().getStringList("database.tables." + tableName);
      if (maps != null && maps.contains(mapName)) {
        maps.remove(mapName);
        plugin.getConfig().set("database.tables." + tableName, maps);
        plugin.saveConfig();
      }
    }
  }

  public static void addTemp(String tableName) {
    tempTable = tableName;
  }

  public static void removeTemp() {
    tempTable = null;
  }

  public static String getTemp() {
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

    // Modificar la lista database.privateMatch
    List<String> privateMaps = plugin.getConfig().getStringList("database.privateMatch");
    if (privateMaps == null) {
      privateMaps = new ArrayList<>();
    }

    if (privateMatch) {
      // Añadir el mapa a privateMatch si no está
      if (!privateMaps.contains(mapName)) {
        privateMaps.add(mapName);
      }
    } else {
      // Eliminar el mapa de privateMatch si está
      privateMaps.remove(mapName);
    }

    plugin.getConfig().set("database.privateMatch", privateMaps);
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
  public static int getDisconnectTime() {
    return disconnectTime;
  }

  public static void setDisconnectTime(int disconnectTime) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    ConfigManager.disconnectTime = disconnectTime;
    plugin.getConfig().set("rankeds.disconnectTime", disconnectTime);
    plugin.saveConfig();
  }

  public static int getRankedMinSize() {
    return rankedMinSize;
  }

  public static void setRankedMinSize(int rankedMinSize) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    ConfigManager.rankedMinSize = rankedMinSize;
    plugin.getConfig().set("rankeds.size.min", rankedMinSize);
    plugin.saveConfig();
  }

  public static int getRankedMaxSize() {
    return rankedMaxSize;
  }

  public static void setRankedMaxSize(int rankedMaxSize) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    ConfigManager.rankedMaxSize = rankedMaxSize;
    plugin.getConfig().set("rankeds.size.max", rankedMaxSize);
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

  public static boolean isRankedTable(String tableName) {
    return rankedTables.contains(tableName);
  }
}
