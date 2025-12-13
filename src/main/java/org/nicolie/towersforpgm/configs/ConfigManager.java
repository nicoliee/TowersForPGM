package org.nicolie.towersforpgm.configs;

import org.bukkit.configuration.file.FileConfiguration;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.tables.DatabaseTablesConfig;

public class ConfigManager {

  private final DatabaseConfig database;
  private final DatabaseTablesConfig databaseTables;
  private final PrivateMatchConfig privateMatch;
  private final RankedConfig ranked;
  private final PreparationTimeConfig preparationTime;
  private final DraftConfig draft;
  private final RefillConfigManager refill;

  private final FileConfiguration config;

  public ConfigManager(TowersForPGM plugin) {
    this.config = plugin.getConfig();

    this.database = new DatabaseConfig(config);
    this.databaseTables = new DatabaseTablesConfig(plugin);
    this.privateMatch = new PrivateMatchConfig(plugin);
    this.ranked = new RankedConfig(plugin);
    this.preparationTime = new PreparationTimeConfig(plugin);
    this.draft = new DraftConfig(plugin);
    this.refill = new RefillConfigManager(plugin);
  }

  public void reload() {
    database.reload();
  }

  public DatabaseConfig database() {
    return database;
  }

  public DatabaseTablesConfig databaseTables() {
    return databaseTables;
  }

  public PrivateMatchConfig privateMatch() {
    return privateMatch;
  }

  public RankedConfig ranked() {
    return ranked;
  }

  public PreparationTimeConfig preparationTime() {
    return preparationTime;
  }

  public DraftConfig draft() {
    return draft;
  }

  public RefillConfigManager refill() {
    return refill;
  }
}
