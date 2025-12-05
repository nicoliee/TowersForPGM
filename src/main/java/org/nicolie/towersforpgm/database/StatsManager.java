package org.nicolie.towersforpgm.database;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.database.models.top.TopResult;
import org.nicolie.towersforpgm.database.sql.SQLStatsManager;
import org.nicolie.towersforpgm.database.sqlite.SQLITEStatsManager;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;

public class StatsManager {

  private static final ExecutorService SQL_EXECUTOR = Executors.newFixedThreadPool(8, r -> {
    Thread t = new Thread(r);
    t.setName("TowersForPGM-SQL-Async");
    t.setDaemon(true);
    return t;
  });

  public static void shutdownSqlExecutor() {
    SQL_EXECUTOR.shutdown();
  }

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
    CompletableFuture.runAsync(
        () -> {
          long startTime = System.currentTimeMillis();
          try {
            if ("MySQL".equals(dbType)) {
              SQLStatsManager.updateStats(table, playerStatsList, eloChange);
            } else if ("SQLite".equals(dbType)) {
              SQLITEStatsManager.updateStats(table, playerStatsList, eloChange);
            } else {
              plugin
                  .getLogger()
                  .warning(
                      "Tipo de base de datos desconocido: " + dbType + " para tabla: " + table);
              return;
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            plugin
                .getLogger()
                .info("[+] updateStats: " + table + ", " + playerStatsList.size() + " players, "
                    + duration + "ms");
          } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            plugin
                .getLogger()
                .severe("[-] updateStats: " + table + ", " + playerStatsList.size() + " players, "
                    + duration + "ms");
          }
        },
        SQL_EXECUTOR);
  }

  public static void applySanction(
      String table, String username, int gamesToAdd, PlayerEloChange eloChange) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (!plugin.getIsDatabaseActivated()) {
      plugin
          .getLogger()
          .warning("Base de datos no activada, no se puede aplicar sanción en tabla: " + table);
      return;
    }

    String dbType = plugin.getCurrentDatabaseType();
    CompletableFuture.runAsync(
        () -> {
          try {
            if ("MySQL".equals(dbType)) {
              org.nicolie.towersforpgm.database.sql.SQLStatsManager.applySanction(
                  table, username, gamesToAdd, eloChange);
            } else if ("SQLite".equals(dbType)) {
              org.nicolie.towersforpgm.database.sqlite.SQLITEStatsManager.applySanction(
                  table, username, gamesToAdd, eloChange);
            } else {
              plugin
                  .getLogger()
                  .warning("Tipo de base de datos desconocido para sanción: " + dbType);
            }
          } catch (Exception e) {
            plugin.getLogger().severe("Error aplicando sanción: " + e.getMessage());
          }
        },
        SQL_EXECUTOR);
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
    return CompletableFuture.supplyAsync(
        () -> {
          long startTime = System.currentTimeMillis();
          try {
            Stats result = null;
            if ("MySQL".equals(dbType)) {
              result = SQLStatsManager.getStats(table, username);
            } else if ("SQLite".equals(dbType)) {
              result = SQLITEStatsManager.getStats(table, username);
            } else {
              plugin
                  .getLogger()
                  .warning(
                      "Tipo de base de datos desconocido: " + dbType + " para tabla: " + table);
              return null;
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            plugin
                .getLogger()
                .info("[+] getStats: " + table + ", " + username + ", " + duration + "ms");
            return result;
          } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            plugin
                .getLogger()
                .severe("[-] getStats: " + table + ", " + username + ", " + duration + "ms");
            return null;
          }
        },
        SQL_EXECUTOR);
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
    return CompletableFuture.supplyAsync(
        () -> {
          long startTime = System.currentTimeMillis();
          try {
            TopResult result = null;
            if ("MySQL".equals(dbType)) {
              result = SQLStatsManager.getTop(table, dbColumn, limit, page);
            } else if ("SQLite".equals(dbType)) {
              result = SQLITEStatsManager.getTop(table, dbColumn, limit, page);
            } else {
              plugin
                  .getLogger()
                  .warning(
                      "Tipo de base de datos desconocido: " + dbType + " para tabla: " + table);
              return new TopResult(java.util.Collections.emptyList(), 0);
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            plugin
                .getLogger()
                .info("[+] getTop: " + table + ", " + dbColumn + ", page=" + page + ", " + duration
                    + "ms");
            return result;
          } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            plugin
                .getLogger()
                .severe("[-] getTop: " + table + ", " + dbColumn + ", page=" + page + ", "
                    + duration + "ms");
            return new TopResult(java.util.Collections.emptyList(), 0);
          }
        },
        SQL_EXECUTOR);
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
    return CompletableFuture.supplyAsync(
        () -> {
          long startTime = System.currentTimeMillis();
          try {
            TopResult result = null;
            if ("MySQL".equals(dbType)) {
              result =
                  SQLStatsManager.getTop(table, dbColumn, limit, lastValue, lastUser, totalRecords);
            } else if ("SQLite".equals(dbType)) {
              result = SQLITEStatsManager.getTop(
                  table, dbColumn, limit, lastValue, lastUser, totalRecords);
            } else {
              plugin
                  .getLogger()
                  .warning(
                      "Tipo de base de datos desconocido: " + dbType + " para tabla: " + table);
              return new TopResult(java.util.Collections.emptyList(), 0);
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            plugin
                .getLogger()
                .info("[+] getTop(keyset): " + table + ", " + dbColumn + ", " + duration + "ms");
            return result;
          } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            plugin
                .getLogger()
                .severe("[-] getTop(keyset): " + table + ", " + dbColumn + ", " + duration + "ms");
            return new TopResult(java.util.Collections.emptyList(), 0);
          }
        },
        SQL_EXECUTOR);
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
    return CompletableFuture.supplyAsync(
        () -> {
          long startTime = System.currentTimeMillis();
          try {
            List<PlayerEloChange> result = null;
            if ("MySQL".equals(dbType)) {
              result = SQLStatsManager.getEloForUsernames(table, usernames);
            } else if ("SQLite".equals(dbType)) {
              plugin.getLogger().warning("Usando SQLite para obtener elos de tabla: " + table);
              result = SQLITEStatsManager.getEloForUsernames(table, usernames);
            } else {
              plugin
                  .getLogger()
                  .warning(
                      "Tipo de base de datos desconocido: " + dbType + " para tabla: " + table);
              return java.util.Collections.emptyList();
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            plugin
                .getLogger()
                .info("[+] getEloForUsernames: " + table + ", " + usernames.size() + " users, "
                    + duration + "ms");
            return result;
          } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            plugin
                .getLogger()
                .severe("[-] getEloForUsernames: " + table + ", " + usernames.size() + " users, "
                    + duration + "ms");
            return java.util.Collections.emptyList();
          }
        },
        SQL_EXECUTOR);
  }

  public static CompletableFuture<List<String>> getAllUsernamesFiltered(String filter) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (!plugin.getIsDatabaseActivated()) {
      plugin.getLogger().warning("Base de datos no activada, no se pueden filtrar usuarios");
      return CompletableFuture.completedFuture(java.util.Collections.emptyList());
    }

    String dbType = plugin.getCurrentDatabaseType();
    return CompletableFuture.supplyAsync(
        () -> {
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
        },
        SQL_EXECUTOR);
  }
}
