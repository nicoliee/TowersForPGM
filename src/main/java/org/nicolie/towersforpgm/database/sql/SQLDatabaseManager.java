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
          + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
      config.setUsername(user);
      config.setPassword(password);
      config.setMaximumPoolSize(50);
      config.setMinimumIdle(20);
      config.setIdleTimeout(60000);
      config.setMaxLifetime(1200000);
      config.setConnectionTimeout(5000);
      config.setLeakDetectionThreshold(2000);

      dataSource = new HikariDataSource(config);
    } catch (Exception e) {
      plugin.getLogger().severe("Error al conectar con la base de datos: " + e.getMessage());
      e.printStackTrace();
    }
  }

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
    return dataSource.getConnection();
  }

  public void disconnect() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
      dataSource = null;
      plugin.getLogger().info("Conexión de MySQL cerrada.");
    }
  }

  public boolean isConnected() {
    return dataSource != null && !dataSource.isClosed();
  }
}
