package org.nicolie.towersforpgm.database.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import org.nicolie.towersforpgm.TowersForPGM;

public class SQLDatabaseManager {
  private final TowersForPGM plugin;
  private HikariDataSource dataSource;

  public SQLDatabaseManager(TowersForPGM plugin) {
    this.plugin = plugin;
  }

  public void connect() {
    try {
      if (!plugin.getConfig().getBoolean("database.enabled")) {
        return;
      }

      String host = plugin.getConfig().getString("database.host");
      int port = plugin.getConfig().getInt("database.port");
      String name = plugin.getConfig().getString("database.name");
      String user = plugin.getConfig().getString("database.user");
      String password = plugin.getConfig().getString("database.password");

      if (host == null || name == null || user == null || password == null) {
        plugin
            .getLogger()
            .warning("Faltan credenciales de la base de datos. No se intentará la conexión.");
        return;
      }

      HikariConfig config = new HikariConfig();
      config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + name
          + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&socketTimeout=30000");
      config.setUsername(user);
      config.setPassword(password);
      // Para servidores pequeños-medianos (< 50 jugadores concurrentes)
      config.setMaximumPoolSize(6); // Reducir de 16
      config.setMinimumIdle(1); // Reducir de 2
      config.setIdleTimeout(300000); // 5 minutos
      config.setMaxLifetime(900000); // 15 minutos
      config.setConnectionTimeout(35000);
      config.addDataSourceProperty("cachePrepStmts", "true");
      config.addDataSourceProperty("prepStmtCacheSize", "250");
      config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
      config.addDataSourceProperty("useServerPrepStmts", "true");
      config.addDataSourceProperty("useLocalSessionState", "true");
      config.addDataSourceProperty("rewriteBatchedStatements", "true");
      config.addDataSourceProperty("cacheResultSetMetadata", "true");
      config.addDataSourceProperty("cacheServerConfiguration", "true");
      config.addDataSourceProperty("elideSetAutoCommits", "true");
      config.addDataSourceProperty("maintainTimeStats", "false");
      config.addDataSourceProperty("tcpKeepAlive", "true");
      config.setValidationTimeout(5000);
      config.setConnectionTestQuery("SELECT 1");
      config.setLeakDetectionThreshold(15000);

      dataSource = new HikariDataSource(config);
    } catch (Exception e) {
      plugin.getLogger().severe("Error al conectar con la base de datos: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** Obtiene una conexión del pool. Si el pool está cerrado, intenta reconectar de forma segura. */
  public Connection getConnection() throws SQLException {
    if (dataSource == null || dataSource.isClosed()) {
      synchronized (this) {
        if (dataSource == null || dataSource.isClosed()) {
          plugin.getLogger().warning("El pool de conexiones está cerrado. Reconectando...");
          connect();
        }
      }
    }
    if (dataSource == null) {
      throw new SQLException("No se pudo establecer conexión con la base de datos");
    }
    Connection connection = dataSource.getConnection();
    if (connection == null || !connection.isValid(2)) {
      if (connection != null) connection.close();
      throw new SQLException("La conexión obtenida no es válida.");
    }
    return connection;
  }

  /** Cierra el pool de conexiones y limpia la referencia. */
  public void disconnect() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
      dataSource = null;
      plugin.getLogger().info("Conexión de MySQL cerrada.");
    }
  }

  /** Verifica si el pool de conexiones está activo. */
  public boolean isConnected() {
    return dataSource != null && !dataSource.isClosed();
  }
}
