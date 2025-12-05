package org.nicolie.towersforpgm.configs;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.nicolie.towersforpgm.preparationTime.MatchConfig;
import org.nicolie.towersforpgm.preparationTime.Region;

public class PreparationTimeConfig {
  private final JavaPlugin plugin;
  private final Map<String, Region> regions = new HashMap<>();
  private Map<String, MatchConfig> matchConfigs = new HashMap<>();
  private boolean preparationEnabled = true;

  public PreparationTimeConfig(JavaPlugin plugin) {
    this.plugin = plugin;
    load();
  }

  public void load() {
    // Cargar estado de preparationEnabled
    preparationEnabled = plugin.getConfig().getBoolean("preparationTime.enabled", true);

    ConfigurationSection section =
        plugin.getConfig().getConfigurationSection("preparationTime.maps");
    if (section == null) {
      plugin.getLogger().warning("Preparation time maps not found.");
      return;
    }

    // Limpiar datos en memoria antes de cargar
    regions.clear();

    for (String regionName : section.getKeys(false)) {
      ConfigurationSection regionSection = section.getConfigurationSection(regionName);
      if (regionSection == null) continue;

      // Obtener P1 y P2
      Location p1 = parseLocation(regionSection.getString("P1"));
      Location p2 = parseLocation(regionSection.getString("P2"));

      if (p1 == null || p2 == null) {
        plugin.getLogger().warning("Error loading region: " + regionName);
        continue;
      }

      int timer = regionSection.getInt("Timer", 0);
      int haste = regionSection.getInt("Haste", 0);

      Region region = new Region(p1, p2, timer, haste);
      regions.put(regionName, region);
    }
  }

  private void save() {
    plugin.saveConfig();
  }

  private Location parseLocation(String input) {
    if (input == null) return null;
    String[] parts = input.split(", ");
    if (parts.length != 3) return null;

    try {
      double x = Double.parseDouble(parts[0]);
      double y = Double.parseDouble(parts[1]);
      double z = Double.parseDouble(parts[2]);
      return new Location(Bukkit.getWorlds().get(0), x, y, z);
    } catch (NumberFormatException e) {
      plugin.getLogger().warning("Invalid coordinates: " + input);
      return null;
    }
  }

  public Map<String, Region> getRegions() {
    return regions;
  }

  public void storeMatchConfig(String worldName, MatchConfig matchConfig) {
    matchConfigs.put(worldName, matchConfig);
  }

  public void removeMatchConfig(String worldName) {
    matchConfigs.remove(worldName);
  }

  public MatchConfig getMatchConfig(String worldName) {
    return matchConfigs.get(worldName);
  }

  public boolean isPreparationEnabled() {
    return preparationEnabled;
  }

  public void setPreparationEnabled(boolean enabled) {
    preparationEnabled = enabled;
    plugin.getConfig().set("preparationTime.enabled", enabled);
    save();
  }

  public void addRegion(String mapName, Region region) {
    // Actualizar memoria
    regions.put(mapName, region);

    // Actualizar config.yml
    String basePath = "preparationTime.maps." + mapName;
    plugin.getConfig().set(basePath + ".P1", locationToString(region.getP1()));
    plugin.getConfig().set(basePath + ".P2", locationToString(region.getP2()));
    plugin.getConfig().set(basePath + ".Timer", region.getTimer());
    plugin.getConfig().set(basePath + ".Haste", region.getHaste());
    save();
  }

  public void removeRegion(String mapName) {
    // Eliminar de memoria
    regions.remove(mapName);

    // Eliminar de config.yml
    plugin.getConfig().set("preparationTime.maps." + mapName, null);
    save();
  }

  public void updateRegionP1(String mapName, Location p1) {
    Region region = regions.get(mapName);
    if (region != null) {
      // Actualizar memoria
      region.setP1(p1);

      // Actualizar config.yml
      plugin.getConfig().set("preparationTime.maps." + mapName + ".P1", locationToString(p1));
      save();
    }
  }

  public void updateRegionP2(String mapName, Location p2) {
    Region region = regions.get(mapName);
    if (region != null) {
      // Actualizar memoria
      region.setP2(p2);

      // Actualizar config.yml
      plugin.getConfig().set("preparationTime.maps." + mapName + ".P2", locationToString(p2));
      save();
    }
  }

  public void updateRegionTimer(String mapName, int timer) {
    Region region = regions.get(mapName);
    if (region != null) {
      // Actualizar memoria
      region.setTimer(timer);

      // Actualizar config.yml
      plugin.getConfig().set("preparationTime.maps." + mapName + ".Timer", timer);
      save();
    }
  }

  public void updateRegionHaste(String mapName, int haste) {
    Region region = regions.get(mapName);
    if (region != null) {
      // Actualizar memoria
      region.setHaste(haste);

      // Actualizar config.yml
      plugin.getConfig().set("preparationTime.maps." + mapName + ".Haste", haste);
      save();
    }
  }

  private String locationToString(Location location) {
    if (location == null) return null;
    return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
  }

  public boolean hasRegion(String mapName) {
    return regions.containsKey(mapName);
  }

  public Region getRegion(String mapName) {
    return regions.get(mapName);
  }

  public void updateMatchConfig(String worldName, MatchConfig matchConfig) {
    if (matchConfig != null) {
      matchConfigs.put(worldName, matchConfig);
    } else {
      matchConfigs.remove(worldName);
    }
  }

  @Override
  public String toString() {
    return "PreparationTimeConfig{" + "plugin="
        + plugin + ", regions="
        + regions + ", matchConfigs="
        + matchConfigs + ", preparationEnabled="
        + preparationEnabled + '}';
  }
}
