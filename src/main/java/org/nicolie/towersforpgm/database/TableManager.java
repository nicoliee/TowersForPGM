package org.nicolie.towersforpgm.database;

import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.sql.SQLTableManager;
import org.nicolie.towersforpgm.database.sqlite.SQLITETableManager;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;

public class TableManager {

  public static void createTable(String tableName) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (!plugin.getIsDatabaseActivated()) {
      plugin
          .getLogger()
          .warning("Base de datos no activada, no se puede crear tabla: " + tableName);
      return;
    }

    String dbType = plugin.getCurrentDatabaseType();
    try {
      if ("MySQL".equals(dbType)) {
        SQLTableManager.createTable(tableName);
      } else if ("SQLite".equals(dbType)) {
        SQLITETableManager.createTable(tableName);
      } else {
        plugin
            .getLogger()
            .warning("Tipo de base de datos desconocido: " + dbType + " para tabla: " + tableName);
      }
    } catch (Exception e) {
      plugin.getLogger().severe("Error creando tabla " + tableName + ": " + e.getMessage());
    }
  }

  public static void createDCAccountsTable() {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (!plugin.getIsDatabaseActivated()) {
      plugin.getLogger().warning("Base de datos no activada, no se puede crear tabla DCAccounts");
      return;
    }
    System.out.println("Attempting to create DCAccounts table...");

    if (!MatchBotConfig.isRankedEnabled()) {
      System.out.println("Ranked system not enabled, skipping DCAccounts table creation.");
      return;
    }
    String dbType = plugin.getCurrentDatabaseType();
    try {
      if ("MySQL".equals(dbType)) {
        System.out.println("Creating DCAccounts table in MySQL database...");
        SQLTableManager.createDCAccountsTable();
      } else if ("SQLite".equals(dbType)) {
        System.out.println("Creating DCAccounts table in SQLite database...");
        SQLITETableManager.createDCAccountsTable();
      } else {
        plugin
            .getLogger()
            .warning("Tipo de base de datos desconocido: " + dbType + " para tabla DCAccounts");
      }
    } catch (Exception e) {
      plugin.getLogger().severe("Error creando tabla DCAccounts: " + e.getMessage());
    }
  }

  public static void createHistoryTables() {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (!plugin.getIsDatabaseActivated()) {
      plugin
          .getLogger()
          .warning("Base de datos no activada, no se pueden crear tablas de historial");
      return;
    }

    String dbType = plugin.getCurrentDatabaseType();
    try {
      if ("MySQL".equals(dbType)) {
        org.nicolie.towersforpgm.database.sql.SQLTableManager.createHistoryTables();
      } else if ("SQLite".equals(dbType)) {
        org.nicolie.towersforpgm.database.sqlite.SQLITETableManager.createHistoryTables();
      } else {
        plugin
            .getLogger()
            .warning("Tipo de base de datos desconocido: " + dbType + " para tablas de historial");
      }
    } catch (Exception e) {
      plugin.getLogger().severe("Error creando tablas de historial: " + e.getMessage());
    }
  }
}
