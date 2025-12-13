package org.nicolie.towersforpgm.configs;

import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class RefillConfigManager {
  private final JavaPlugin plugin;
  private File refillFile;
  private FileConfiguration refillConfig;

  public RefillConfigManager(JavaPlugin plugin) {
    this.plugin = plugin;
    load();
  }

  public void load() {
    refillFile = new File(plugin.getDataFolder(), "refill.yml");
    if (!refillFile.exists()) {
      plugin.saveResource("refill.yml", false);
    }
    refillConfig = YamlConfiguration.loadConfiguration(refillFile);
  }

  public FileConfiguration config() {
    return refillConfig;
  }

  public void save() {
    try {
      refillConfig.save(refillFile);
    } catch (IOException e) {
      plugin.getLogger().severe("Error al guardar refill.yml: " + e.getMessage());
    }
  }

  public void reload() {
    refillConfig = YamlConfiguration.loadConfiguration(refillFile);
  }
}
