package org.nicolie.towersforpgm.database;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.sql.SQLStatsManager;
import org.nicolie.towersforpgm.database.sqlite.SQLITEStatsManager;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;

public class StatsManager {

  public static void updateStats(
      String table, List<Stats> playerStatsList, List<PlayerEloChange> eloChange) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (!plugin.getIsDatabaseActivated()) {
      plugin
          .getLogger()
          .warning(
              "Base de datos no activada, no se pueden actualizar estadísticas en tabla: " + table);
      return;
    }

    String dbType = plugin.getCurrentDatabaseType();
    try {
      if ("MySQL".equals(dbType)) {
        SQLStatsManager.updateStats(table, playerStatsList, eloChange);
      } else if ("SQLite".equals(dbType)) {
        SQLITEStatsManager.updateStats(table, playerStatsList, eloChange);
      } else {
        plugin
            .getLogger()
            .warning("Tipo de base de datos desconocido: " + dbType + " para tabla: " + table);
      }
    } catch (Exception e) {
      plugin
          .getLogger()
          .severe("Error actualizando estadísticas en tabla " + table + ": " + e.getMessage());
    }
  }

  public static CompletableFuture<Stats> getStats(String table, String username) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (!plugin.getIsDatabaseActivated()) {
      plugin
          .getLogger()
          .warning(
              "Base de datos no activada, no se pueden obtener estadísticas de tabla: " + table);
      return CompletableFuture.completedFuture(null);
    }

    String dbType = plugin.getCurrentDatabaseType();
    try {
      if ("MySQL".equals(dbType)) {
        return org.nicolie.towersforpgm.database.sql.SQLStatsManager.getStats(table, username);
      } else if ("SQLite".equals(dbType)) {
        return org.nicolie.towersforpgm.database.sqlite.SQLITEStatsManager.getStats(
            table, username);
      } else {
        plugin
            .getLogger()
            .warning("Tipo de base de datos desconocido: " + dbType + " para tabla: " + table);
        return CompletableFuture.completedFuture(null);
      }
    } catch (Exception e) {
      plugin
          .getLogger()
          .severe("Error obteniendo estadísticas de tabla " + table + ": " + e.getMessage());
      return CompletableFuture.completedFuture(null);
    }
  }

  public static CompletableFuture<TopResult> getTop(
      String table, String dbColumn, int limit, int page) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (!plugin.getIsDatabaseActivated()) {
      plugin
          .getLogger()
          .warning("Base de datos no activada, no se puede obtener top de tabla: " + table);
      return CompletableFuture.completedFuture(new TopResult(java.util.Collections.emptyList(), 0));
    }

    String dbType = plugin.getCurrentDatabaseType();
    try {
      if ("MySQL".equals(dbType)) {
        return SQLStatsManager.getTop(table, dbColumn, limit, page);
      } else if ("SQLite".equals(dbType)) {
        return SQLITEStatsManager.getTop(table, dbColumn, limit, page);
      } else {
        plugin
            .getLogger()
            .warning("Tipo de base de datos desconocido: " + dbType + " para tabla: " + table);
        return CompletableFuture.completedFuture(
            new TopResult(java.util.Collections.emptyList(), 0));
      }
    } catch (Exception e) {
      plugin.getLogger().severe("Error obteniendo top de tabla " + table + ": " + e.getMessage());
      return CompletableFuture.completedFuture(new TopResult(java.util.Collections.emptyList(), 0));
    }
  }

  public static CompletableFuture<TopResult> getTop(
      String table,
      String dbColumn,
      int limit,
      Double lastValue,
      String lastUser,
      Integer totalRecords) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (!plugin.getIsDatabaseActivated()) {
      plugin
          .getLogger()
          .warning("Base de datos no activada, no se puede obtener top de tabla: " + table);
      return CompletableFuture.completedFuture(new TopResult(java.util.Collections.emptyList(), 0));
    }

    String dbType = plugin.getCurrentDatabaseType();
    try {
      if ("MySQL".equals(dbType)) {
        return SQLStatsManager.getTop(table, dbColumn, limit, lastValue, lastUser, totalRecords);
      } else if ("SQLite".equals(dbType)) {
        return SQLITEStatsManager.getTop(table, dbColumn, limit, lastValue, lastUser, totalRecords);
      } else {
        plugin
            .getLogger()
            .warning("Tipo de base de datos desconocido: " + dbType + " para tabla: " + table);
        return CompletableFuture.completedFuture(
            new TopResult(java.util.Collections.emptyList(), 0));
      }
    } catch (Exception e) {
      plugin.getLogger().severe("Error obteniendo top de tabla " + table + ": " + e.getMessage());
      return CompletableFuture.completedFuture(new TopResult(java.util.Collections.emptyList(), 0));
    }
  }

  public static CompletableFuture<List<PlayerEloChange>> getEloForUsernames(
      String table, List<String> usernames) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (!plugin.getIsDatabaseActivated()) {
      plugin
          .getLogger()
          .warning("Base de datos no activada, no se pueden obtener elos de tabla: " + table);
      return CompletableFuture.completedFuture(java.util.Collections.emptyList());
    }

    String dbType = plugin.getCurrentDatabaseType();
    try {
      if ("MySQL".equals(dbType)) {
        return SQLStatsManager.getEloForUsernames(table, usernames);
      } else if ("SQLite".equals(dbType)) {
        plugin.getLogger().warning("Usando SQLite para obtener elos de tabla: " + table);
        return SQLITEStatsManager.getEloForUsernames(table, usernames);
      } else {
        plugin
            .getLogger()
            .warning("Tipo de base de datos desconocido: " + dbType + " para tabla: " + table);
        return CompletableFuture.completedFuture(java.util.Collections.emptyList());
      }
    } catch (Exception e) {
      return CompletableFuture.completedFuture(java.util.Collections.emptyList());
    }
  }

  public static List<String> getAllUsernamesFiltered(String filter) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (!plugin.getIsDatabaseActivated()) {
      plugin.getLogger().warning("Base de datos no activada, no se pueden filtrar usuarios");
      return java.util.Collections.emptyList();
    }

    String dbType = plugin.getCurrentDatabaseType();
    try {
      if ("MySQL".equals(dbType)) {
        return SQLStatsManager.getAllUsernamesFiltered(filter);
      } else if ("SQLite".equals(dbType)) {
        return SQLITEStatsManager.getAllUsernamesFiltered(filter);
      } else {
        plugin.getLogger().warning("Tipo de base de datos desconocido: " + dbType);
        return java.util.Collections.emptyList();
      }
    } catch (Exception e) {
      plugin.getLogger().severe("Error filtrando usuarios: " + e.getMessage());
      return java.util.Collections.emptyList();
    }
  }
}
