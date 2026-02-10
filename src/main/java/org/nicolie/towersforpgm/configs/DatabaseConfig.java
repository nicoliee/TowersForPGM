package org.nicolie.towersforpgm.configs;

import org.bukkit.configuration.file.FileConfiguration;

public class DatabaseConfig {

  private final FileConfiguration config;

  private boolean enabled;
  private String host;
  private int port;
  private String name;
  private String user;
  private String password;
  private String id;

  public DatabaseConfig(FileConfiguration config) {
    this.config = config;
    reload();
  }

  public void reload() {
    this.enabled = config.getBoolean("database.enabled", false);

    this.host = config.getString("database.host", "localhost");
    this.port = config.getInt("database.port", 3306);
    this.name = config.getString("database.name", "database");
    this.user = config.getString("database.user", "root");
    this.password = config.getString("database.password", "");
    this.id = config.getString("database.id", "server1");
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getDatabaseName() {
    return name;
  }

  public String getUser() {
    return user;
  }

  public String getPassword() {
    return password;
  }

  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return "DatabaseConfig{" + "enabled="
        + enabled + ", host='"
        + host + '\'' + ", port="
        + port + ", name='"
        + name + '\'' + ", user='"
        + user + '\'' + ", id='"
        + id + '\'' + '}';
  }
}
