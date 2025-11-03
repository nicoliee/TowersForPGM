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

  /** Crea la tabla DCAccounts para vincular cuentas Discord-Minecraft */
  public static void createDCAccountsTable() {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (!plugin.getIsDatabaseActivated()) {
      plugin.getLogger().warning("Base de datos no activada, no se puede crear tabla DCAccounts");
      return;
    }

    if (!MatchBotConfig.isRankedEnabled()) {
      return;
    }
    String dbType = plugin.getCurrentDatabaseType();
    try {
      if ("MySQL".equals(dbType)) {
        SQLTableManager.createDCAccountsTable();
      } else if ("SQLite".equals(dbType)) {
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
}
