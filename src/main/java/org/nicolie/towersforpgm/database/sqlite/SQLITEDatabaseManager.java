package org.nicolie.towersforpgm.database.sqlite;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import org.nicolie.towersforpgm.TowersForPGM;

public class SQLITEDatabaseManager {
  private final TowersForPGM plugin;
  private HikariDataSource dataSource;

  public SQLITEDatabaseManager(TowersForPGM plugin) {
    this.plugin = plugin;
  }

  public void connect() {
    try {

      String databasePath = "plugins/TowersForPGM/database.db";

      File dbFile = new File(databasePath);
      File parentDir = dbFile.getParentFile();
      if (parentDir != null && !parentDir.exists()) {
        parentDir.mkdirs();
      }

      HikariConfig config = new HikariConfig();
      config.setJdbcUrl("jdbc:sqlite:" + databasePath);

      config.setMaximumPoolSize(5);
      config.setMinimumIdle(1);
      config.setIdleTimeout(60000);
      config.setMaxLifetime(300000);
      config.setConnectionTimeout(30000);
      config.setLeakDetectionThreshold(60000);
      config.setConnectionTestQuery("SELECT 1");
      config.setValidationTimeout(5000);
      config.setInitializationFailTimeout(10000);

      dataSource = new HikariDataSource(config);

      try (Connection conn = dataSource.getConnection()) {
        conn.createStatement().execute("PRAGMA foreign_keys = ON");
        conn.createStatement().execute("PRAGMA journal_mode = WAL");
        conn.createStatement().execute("PRAGMA synchronous = NORMAL");
        conn.createStatement().execute("PRAGMA cache_size = 10000");
        conn.createStatement().execute("PRAGMA temp_store = MEMORY");
        conn.createStatement().execute("PRAGMA mmap_size = 268435456");
        conn.createStatement()
            .execute("PRAGMA busy_timeout = 30000"); // 30 segundos de espera para locks
        conn.createStatement().execute("PRAGMA wal_autocheckpoint = 1000");
      }

    } catch (Exception e) {
      plugin.getLogger().severe("Error al conectar con la base de datos SQLite: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public Connection getConnection() throws SQLException {
    if (dataSource == null || dataSource.isClosed()) {
      plugin.getLogger().warning("El pool de conexiones SQLite está cerrado. Reconectando...");
      connect();
    }

    try {
      Connection connection = dataSource.getConnection();
      // Para SQLite, simplemente verificar que la conexión no esté cerrada
      // ya que isValid() no está implementado en el driver SQLite
      if (connection.isClosed()) {
        throw new SQLException("La conexión SQLite obtenida está cerrada.");
      }
      return connection;
    } catch (SQLException e) {
      plugin.getLogger().severe("Error conectando a la base de datos SQLite");
      throw e;
    }
  }

  public void disconnect() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
      plugin.getLogger().info("Conexión de SQLite cerrada.");
    }
  }
}
