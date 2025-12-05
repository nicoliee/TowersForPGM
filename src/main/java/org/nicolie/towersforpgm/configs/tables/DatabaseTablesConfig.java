package org.nicolie.towersforpgm.configs.tables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.nicolie.towersforpgm.utils.MatchManager;

public class DatabaseTablesConfig {

  private final JavaPlugin plugin;
  private final Map<String, TableInfo> tables = new HashMap<>();
  private String defaultTable;
  private String tempTable;
  private String rankedDefaultTable;

  public DatabaseTablesConfig(JavaPlugin plugin) {
    this.plugin = plugin;
    load();
  }

  public void reload() {
    plugin.reloadConfig();
    load();
  }

  public void load() {
    tables.clear();
    defaultTable = plugin.getConfig().getString("database.defaultTable", "default");

    ConfigurationSection tablesSection =
        plugin.getConfig().getConfigurationSection("database.tables");
    if (tablesSection != null) {
      loadTables(tablesSection);
    }
  }

  private void loadTables(ConfigurationSection tablesSection) {
    Set<String> rankedTableNames =
        new HashSet<>(plugin.getConfig().getStringList("rankeds.tables"));

    for (String tableName : tablesSection.getKeys(false)) {
      List<String> maps = plugin.getConfig().getStringList("database.tables." + tableName);
      boolean isRanked = rankedTableNames.contains(tableName);
      tables.put(tableName, new TableInfo(maps != null ? maps : new ArrayList<>(), isRanked));
    }
    for (String rankedTableName : rankedTableNames) {
      tables.putIfAbsent(rankedTableName, new TableInfo(new ArrayList<>(), true));
    }

    rankedDefaultTable = plugin.getConfig().getString("rankeds.defaultTable", "RankedT0");
  }

  private void save() {
    plugin.saveConfig();
  }

  // =========================
  // GETTERS GENERALES
  // =========================

  public TableInfo getTableInfo(String tableName) {
    return tables.get(tableName);
  }

  public Set<String> getTables(TableType type) {
    return tables.entrySet().stream()
        .filter(entry -> type == TableType.ALL
            || (type == TableType.RANKED) == entry.getValue().isRanked())
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  // =========================
  // RANKED
  // =========================

  public boolean currentTableIsRanked() {
    String currentTable = getTable(MatchManager.getMatch().getMap().getName());
    TableInfo tableInfo = tables.get(currentTable);
    return tableInfo != null && tableInfo.isRanked();
  }

  public String getRankedDefaultTable() {
    return rankedDefaultTable;
  }

  public void setRankedDefaultTable(String table) {
    this.rankedDefaultTable = table;
    plugin.getConfig().set("rankeds.defaultTable", table);
    save();
  }

  private void updateRankedTablesConfig() {
    List<String> rankedTableNames = tables.entrySet().stream()
        .filter(entry -> entry.getValue().isRanked())
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());

    plugin.getConfig().set("rankeds.tables", rankedTableNames);
  }

  // =========================
  // TABLAS (CRUD)
  // =========================

  public void addTable(String table, boolean isRanked) {
    if (!tables.containsKey(table)) {
      tables.put(table, new TableInfo(new ArrayList<>(), isRanked));
      updateConfigForTable(table);
    }
  }

  public void removeTable(String table) {
    if (tables.remove(table) != null) {
      plugin.getConfig().set("database.tables." + table, null);
      updateRankedTablesConfig();
      save();
    }
  }

  private void updateConfigForTable(String tableName) {
    TableInfo tableInfo = tables.get(tableName);
    if (tableInfo != null) {
      plugin.getConfig().set("database.tables." + tableName, tableInfo.getMaps());
      updateRankedTablesConfig();
      save();
    }
  }

  // =========================
  // MAPAS DENTRO DE TABLAS
  // =========================

  public void addMapToTable(String map, String table) {
    if (isValidInput(map, table)) {
      TableInfo tableInfo =
          tables.computeIfAbsent(table, k -> new TableInfo(new ArrayList<>(), false));
      if (tableInfo.addMap(map)) {
        updateConfigForTable(table);
      }
    }
  }

  public void removeMapFromTable(String map, String table) {
    TableInfo tableInfo = tables.get(table);
    if (tableInfo != null && tableInfo.removeMap(map)) {
      updateConfigForTable(table);
    }
  }

  public void removeMap(String map) {
    boolean changed = false;

    for (Map.Entry<String, TableInfo> entry : tables.entrySet()) {
      if (entry.getValue().removeMap(map)) {
        updateConfigForTable(entry.getKey());
        changed = true;
      }
    }

    if (changed) {
      save();
    }
  }

  private boolean isValidInput(String... inputs) {
    for (String input : inputs) {
      if (input == null || input.trim().isEmpty()) {
        return false;
      }
    }
    return true;
  }

  // =========================
  // TABLA ACTUAL (DEFAULT / TEMP)
  // =========================

  public String getTable(String map) {
    if (tempTable != null && !tempTable.isEmpty()) {
      return tempTable;
    }

    if (map == null) {
      return defaultTable;
    }

    return tables.entrySet().stream()
        .filter(entry -> entry.getValue().getMaps().contains(map))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(defaultTable);
  }

  public String getDefaultTable() {
    return defaultTable;
  }

  public void setDefaultTable(String table) {
    this.defaultTable = table;
    plugin.getConfig().set("database.defaultTable", table);
    save();
  }

  public void setTempTable(String table) {
    this.tempTable = table;
  }

  public void removeTempTable() {
    this.tempTable = null;
  }

  public String getTempTable() {
    return tempTable;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("DatabaseTablesConfig{\n");
    sb.append("  defaultTable='").append(defaultTable).append("'\n");
    sb.append("  tempTable='").append(tempTable != null ? tempTable : "none").append("'\n");
    sb.append("  tables={\n");

    if (tables.isEmpty()) {
      sb.append("    (empty)\n");
    } else {
      tables.forEach((tableName, tableInfo) -> {
        sb.append("    '")
            .append(tableName)
            .append("' (")
            .append(tableInfo.isRanked() ? "ranked" : "unranked")
            .append("): [");

        List<String> maps = tableInfo.getMaps();
        if (maps.isEmpty()) {
          sb.append("(no maps)");
        } else {
          sb.append(String.join(", ", maps));
        }

        sb.append("]\n");
      });
    }

    sb.append("  }\n");
    sb.append("  totalTables=").append(tables.size());
    sb.append(", rankedTables=").append(getTables(TableType.RANKED).size());
    sb.append("\n}");

    return sb.toString();
  }
}
