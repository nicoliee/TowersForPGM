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

        // Verificar si la tabla ya existe
        if (tableExists(conn, tableName)) {
          return;
        }

        // La tabla no existe, crearla completa
        String createTableSQL = buildCreateTableSQL(tableName);
        stmt.executeUpdate(createTableSQL);

        TowersForPGM.getInstance()
            .getLogger()
            .info("Tabla '" + tableName + "' creada exitosamente");

      } catch (SQLException e) {
        TowersForPGM.getInstance()
            .getLogger()
            .log(Level.SEVERE, "Error creando tabla '" + tableName + "' en SQLite", e);
      }
    });
  }

  public static void verifyStatsTable(String tableName, boolean isRanked) {
    if ("none".equalsIgnoreCase(tableName)
        || tableName == null
        || tableName.trim().isEmpty()) {
      return;
    }

    Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
      try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
          Statement stmt = conn.createStatement()) {

        // Verificar si la tabla existe
        if (!tableExists(conn, tableName)) {
          TowersForPGM.getInstance()
              .getLogger()
              .warning("Tabla '" + tableName + "' no existe, no se puede verificar");
          return;
        }

        // En SQLite no hay columnas generadas como en MySQL
        // Las columnas calculadas se crean como columnas normales
        // Por ahora, simplemente verificamos que la tabla existe
        TowersForPGM.getInstance()
            .getLogger()
            .info("Tabla '" + tableName + "' verificada exitosamente");

      } catch (SQLException e) {
        TowersForPGM.getInstance()
            .getLogger()
            .log(Level.SEVERE, "Error verificando tabla '" + tableName + "' en SQLite", e);
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

  /** Crea tablas de historial de partidas (SQLite) */
  public static void createHistoryTables() {
    Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
      try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection();
          Statement stmt = conn.createStatement()) {

        String createMatches = "CREATE TABLE IF NOT EXISTS matches_history ("
            + "match_id TEXT PRIMARY KEY,"
            + "table_name TEXT NOT NULL,"
            + "map_name TEXT NOT NULL,"
            + "duration_seconds INTEGER NOT NULL,"
            + "ranked INTEGER NOT NULL DEFAULT 0,"
            + "scores_text TEXT,"
            + "winners_text TEXT,"
            + "finished_at INTEGER NOT NULL,"
            + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP"
            + ")";

        String createPlayers = "CREATE TABLE IF NOT EXISTS match_players_history ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "match_id TEXT NOT NULL,"
            + "username TEXT NOT NULL,"
            + "kills INTEGER DEFAULT 0,"
            + "deaths INTEGER DEFAULT 0,"
            + "assists INTEGER DEFAULT 0,"
            + "damageDone REAL DEFAULT 0,"
            + "damageTaken REAL DEFAULT 0,"
            + "points INTEGER DEFAULT 0,"
            + "win INTEGER DEFAULT 0,"
            + "game INTEGER DEFAULT 1,"
            + "winstreak_delta INTEGER DEFAULT 0,"
            + "elo_delta INTEGER DEFAULT 0,"
            + "maxElo_after INTEGER DEFAULT 0,"
            + "FOREIGN KEY (match_id) REFERENCES matches_history(match_id) ON DELETE CASCADE"
            + ")";
        stmt.executeUpdate(createMatches);
        stmt.executeUpdate(createPlayers);

        // Índices para rendimiento
        stmt.executeUpdate(
            "CREATE INDEX IF NOT EXISTS idx_match_history_table_date ON matches_history(table_name, created_at)");
        stmt.executeUpdate(
            "CREATE INDEX IF NOT EXISTS idx_match_players_match ON match_players_history(match_id)");
        stmt.executeUpdate(
            "CREATE INDEX IF NOT EXISTS idx_match_players_user ON match_players_history(username)");

      } catch (SQLException e) {
        TowersForPGM.getInstance()
            .getLogger()
            .log(Level.SEVERE, "Error creando tablas de historial (SQLite)", e);
      }
    });
  }

  private static boolean tableExists(Connection conn, String tableName) throws SQLException {
    String query = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
    try (java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
      stmt.setString(1, tableName);
      try (java.sql.ResultSet rs = stmt.executeQuery()) {
        return rs.next();
      }
    }
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
