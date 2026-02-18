package org.nicolie.towersforpgm.database.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.tables.TableType;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;

public class SQLITETableManager {
  private static final TowersForPGM plugin = TowersForPGM.getInstance();

  public static void createTable(String tableName) {
    if ("none".equalsIgnoreCase(tableName)
        || tableName == null
        || tableName.trim().isEmpty()) {
      return;
    }

    boolean isValidTable =
        plugin.config().databaseTables().getTables(TableType.ALL).contains(tableName);

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
          // Si existe, asegurar que tenga las columnas maxKills, maxPoints y wlRatio
          try {
            stmt.executeUpdate(
                "ALTER TABLE " + tableName + " ADD COLUMN maxKills INTEGER DEFAULT 0");
            TowersForPGM.getInstance()
                .getLogger()
                .info("Columna maxKills agregada a tabla '" + tableName + "'");
          } catch (SQLException e) {
            // Columna ya existe, ignorar
          }

          try {
            stmt.executeUpdate(
                "ALTER TABLE " + tableName + " ADD COLUMN maxPoints INTEGER DEFAULT 0");
            TowersForPGM.getInstance()
                .getLogger()
                .info("Columna maxPoints agregada a tabla '" + tableName + "'");
          } catch (SQLException e) {
            // Columna ya existe, ignorar
          }

          try {
            stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN wlRatio REAL DEFAULT 0");
            TowersForPGM.getInstance()
                .getLogger()
                .info("Columna wlRatio agregada a tabla '" + tableName + "'");
          } catch (SQLException e) {
            // Columna ya existe, ignorar
          }
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

        // Verificar y agregar columnas maxKills y maxPoints si no existen
        // try {
        //   stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN maxKills INTEGER DEFAULT
        // 0");
        //   TowersForPGM.getInstance()
        //       .getLogger()
        //       .info("Columna maxKills agregada a tabla '" + tableName + "'");
        // } catch (SQLException e) {
        //   // Columna ya existe, ignorar
        // }

        // try {
        //   stmt.executeUpdate(
        //       "ALTER TABLE " + tableName + " ADD COLUMN maxPoints INTEGER DEFAULT 0");
        //   TowersForPGM.getInstance()
        //       .getLogger()
        //       .info("Columna maxPoints agregada a tabla '" + tableName + "'");
        // } catch (SQLException e) {
        //   // Columna ya existe, ignorar
        // }

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
    // if (!MatchBotConfig.isVoiceChatEnabled()) {
    //   System.out.println("[SQLite] Ranked system not enabled, skipping DCAccounts table
    // creation.");
    //   return;
    // }
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
            // K/D stats
            + "kills INTEGER DEFAULT 0,"
            + "deaths INTEGER DEFAULT 0,"
            + "assists INTEGER DEFAULT 0,"
            + "killstreak INTEGER DEFAULT 0,"
            + "max_killstreak INTEGER DEFAULT 0,"
            // Bow stats
            + "longest_bow_kill INTEGER DEFAULT 0,"
            + "bow_damage REAL DEFAULT 0,"
            + "bow_damage_taken REAL DEFAULT 0,"
            + "shots_taken INTEGER DEFAULT 0,"
            + "shots_hit INTEGER DEFAULT 0,"
            // Damage stats
            + "damage_done REAL DEFAULT 0,"
            + "damage_taken REAL DEFAULT 0,"
            // Objective stats
            + "destroyable_pieces_broken INTEGER DEFAULT 0,"
            + "monuments_destroyed INTEGER DEFAULT 0,"
            + "flags_captured INTEGER DEFAULT 0,"
            + "flag_pickups INTEGER DEFAULT 0,"
            + "cores_leaked INTEGER DEFAULT 0,"
            + "wools_captured INTEGER DEFAULT 0,"
            + "wools_touched INTEGER DEFAULT 0,"
            + "longest_flag_hold_millis INTEGER DEFAULT 0,"
            + "points INTEGER DEFAULT 0,"
            // Match info
            + "win INTEGER DEFAULT 0,"
            + "game INTEGER DEFAULT 1,"
            + "winstreak_delta INTEGER DEFAULT 0,"
            + "elo_delta INTEGER DEFAULT 0,"
            + "maxElo_after INTEGER DEFAULT 0,"
            // Team info
            + "team_name TEXT,"
            + "team_color_hex TEXT,"
            + "team_score INTEGER,"
            + "elo_before INTEGER,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
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
        stmt.executeUpdate(
            "CREATE INDEX IF NOT EXISTS idx_team_name ON match_players_history(team_name)");
        stmt.executeUpdate(
            "CREATE INDEX IF NOT EXISTS idx_elo_before ON match_players_history(elo_before)");

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
    boolean isRanked =
        plugin.config().databaseTables().getTables(TableType.RANKED).contains(tableName);

    StringBuilder sql = new StringBuilder();
    sql.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
    sql.append("username TEXT PRIMARY KEY, ");
    sql.append("kills INTEGER DEFAULT 0, ");
    sql.append("maxKills INTEGER DEFAULT 0, ");
    sql.append("deaths INTEGER DEFAULT 0, ");
    sql.append("assists INTEGER DEFAULT 0, ");
    sql.append("damageDone REAL DEFAULT 0, ");
    sql.append("damageTaken REAL DEFAULT 0, ");
    sql.append("points INTEGER DEFAULT 0, ");
    sql.append("maxPoints INTEGER DEFAULT 0, ");
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
    sql.append("winrate INTEGER DEFAULT 0, ");
    sql.append("wlRatio REAL DEFAULT 0");

    if (isRanked) {
      sql.append(", elo INTEGER DEFAULT 0, ");
      sql.append("lastElo INTEGER DEFAULT 0, ");
      sql.append("maxElo INTEGER DEFAULT 0");
    }

    sql.append(")");
    return sql.toString();
  }
}
