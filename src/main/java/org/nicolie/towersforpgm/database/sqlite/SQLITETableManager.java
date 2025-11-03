package org.nicolie.towersforpgm.database.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.utils.ConfigManager;

public class SQLITETableManager {

  public static void createTable(String tableName) {
    if ("none".equalsIgnoreCase(tableName)
        || tableName == null
        || tableName.trim().isEmpty()) {
      return;
    }

    boolean isValidTable = ConfigManager.getTables().contains(tableName)
        || ConfigManager.getRankedTables().contains(tableName);

    if (!isValidTable) {
      TowersForPGM.getInstance()
          .getLogger()
          .warning("Tabla '" + tableName + "' no está configurada, no se creará");
      return;
    }

    Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
      try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
          Statement stmt = conn.createStatement()) {

        String createTableSQL = buildCreateTableSQL(tableName);
        stmt.executeUpdate(createTableSQL);

        TowersForPGM.getInstance()
            .getLogger()
            .info("Tabla '" + tableName + "' creada/actualizada exitosamente");

      } catch (SQLException e) {
        TowersForPGM.getInstance()
            .getLogger()
            .log(Level.SEVERE, "Error creando tabla '" + tableName + "' en SQLite", e);
      }
    });
  }

  /** Crea la tabla para vincular cuentas Discord-Minecraft */
  public static void createDCAccountsTable() {
    // Solo crear si el sistema ranked está habilitado
    if (!MatchBotConfig.isRankedEnabled()) {
      System.out.println("[SQLite] Ranked system not enabled, skipping DCAccounts table creation.");
      return;
    }
    Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
      try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
          Statement stmt = conn.createStatement()) {

        String acc_table = MatchBotConfig.getAccountsTable();
        String createTableSQL =
            "CREATE TABLE IF NOT EXISTS " + acc_table + " (" + "uuid TEXT PRIMARY KEY, "
                + "discordId TEXT NOT NULL UNIQUE, "
                + "registeredAt DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")";

        stmt.executeUpdate(createTableSQL);

        // Crear índice para discordId
        String createIndexSQL =
            "CREATE INDEX IF NOT EXISTS idx_discordId ON " + acc_table + " (discordId)";
        stmt.executeUpdate(createIndexSQL);

        TowersForPGM.getInstance().getLogger().info("Tabla " + acc_table + " creada exitosamente");

      } catch (SQLException e) {
        TowersForPGM.getInstance()
            .getLogger()
            .log(Level.SEVERE, "Error creando tabla " + MatchBotConfig.getAccountsTable(), e);
      }
    });
  }

  private static String buildCreateTableSQL(String tableName) {
    boolean isRanked = ConfigManager.getRankedTables().contains(tableName);

    StringBuilder sql = new StringBuilder();
    sql.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
    sql.append("username TEXT PRIMARY KEY, ");
    sql.append("kills INTEGER DEFAULT 0, ");
    sql.append("deaths INTEGER DEFAULT 0, ");
    sql.append("assists INTEGER DEFAULT 0, ");
    sql.append("damageDone REAL DEFAULT 0, ");
    sql.append("damageTaken REAL DEFAULT 0, ");
    sql.append("points INTEGER DEFAULT 0, ");
    sql.append("games INTEGER DEFAULT 0, ");
    sql.append("wins INTEGER DEFAULT 0, ");
    sql.append("winstreak INTEGER DEFAULT 0, ");
    sql.append("maxWinstreak INTEGER DEFAULT 0, ");
    sql.append("killsPerGame REAL DEFAULT 0, ");
    sql.append("deathsPerGame REAL DEFAULT 0, ");
    sql.append("assistsPerGame REAL DEFAULT 0, ");
    sql.append("damageDonePerGame REAL DEFAULT 0, ");
    sql.append("damageTakenPerGame REAL DEFAULT 0, ");
    sql.append("pointsPerGame REAL DEFAULT 0, ");
    sql.append("kdRatio REAL DEFAULT 0, ");
    sql.append("winrate INTEGER DEFAULT 0");

    if (isRanked) {
      sql.append(", elo INTEGER DEFAULT 0, ");
      sql.append("lastElo INTEGER DEFAULT 0, ");
      sql.append("maxElo INTEGER DEFAULT 0");
    }

    sql.append(")");
    return sql.toString();
  }
}
