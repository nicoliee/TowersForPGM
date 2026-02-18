package org.nicolie.towersforpgm.configs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.nicolie.towersforpgm.rankeds.RankedProfile;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.rotation.MapPoolManager;

public class RankedConfig {
  private final JavaPlugin plugin;
  private String activeProfileName;
  private Map<String, RankedProfile> profiles;
  private RankedProfile activeProfile;

  public RankedConfig(JavaPlugin plugin) {
    this.plugin = plugin;
    this.profiles = new HashMap<>();
    load();
  }

  public void load() {
    profiles.clear();
    activeProfileName = plugin.getConfig().getString("rankeds.activeProfile", "default");

    ConfigurationSection profilesSection =
        plugin.getConfig().getConfigurationSection("rankeds.profiles");
    if (profilesSection != null) {
      for (String profileName : profilesSection.getKeys(false)) {
        String basePath = "rankeds.profiles." + profileName;

        int min = plugin.getConfig().getInt(basePath + ".min", 4);
        int max = plugin.getConfig().getInt(basePath + ".max", 6);
        int timerWaiting = plugin.getConfig().getInt(basePath + ".timer.waiting", 30);
        int timerMinReached = plugin.getConfig().getInt(basePath + ".timer.minReached", 15);
        int timerFull = plugin.getConfig().getInt(basePath + ".timer.full", 5);
        int timerDisconnect = plugin.getConfig().getInt(basePath + ".timer.disconnect", 30);
        boolean matchmaking = plugin.getConfig().getBoolean(basePath + ".matchmaking", false);
        String order = plugin.getConfig().getString(basePath + ".order", "ABBAAB");
        boolean reroll = plugin.getConfig().getBoolean(basePath + ".reroll", false);
        String table = plugin.getConfig().getString(basePath + ".table", "");
        String mapPool = plugin.getConfig().getString(basePath + ".mapPool", "default");

        RankedProfile profile = new RankedProfile(
            profileName,
            min,
            max,
            timerWaiting,
            timerMinReached,
            timerFull,
            timerDisconnect,
            matchmaking,
            order,
            reroll,
            table,
            mapPool);

        profiles.put(profileName, profile);
      }
    }

    activeProfile = profiles.get(activeProfileName);
    if (activeProfile == null && !profiles.isEmpty()) {
      activeProfile = profiles.values().iterator().next();
      activeProfileName = activeProfile.getName();
    }
  }

  private void save() {
    plugin.saveConfig();
  }

  public RankedProfile getActiveProfile() {
    return activeProfile;
  }

  public String getActiveProfileName() {
    return activeProfileName;
  }

  public void setActiveProfile(String profileName) {
    if (profiles.containsKey(profileName)) {
      this.activeProfile = profiles.get(profileName);
      this.activeProfileName = profileName;
      plugin.getConfig().set("rankeds.activeProfile", profileName);
      save();
    }
  }

  public Set<String> getProfileNames() {
    return profiles.keySet();
  }

  public RankedProfile getProfile(String name) {
    return profiles.get(name);
  }

  public boolean profileExists(String name) {
    return profiles.containsKey(name);
  }

  public int getDisconnectTime() {
    return activeProfile != null ? activeProfile.getTimerDisconnect() : 30;
  }

  public void setDisconnectTime(int disconnectTime) {
    plugin
        .getConfig()
        .set("rankeds.profiles." + activeProfileName + ".timer.disconnect", disconnectTime);
    save();
    load();
  }

  public int getRankedMinSize() {
    return activeProfile != null ? activeProfile.getMin() : 4;
  }

  public void setRankedMinSize(int rankedMinSize) {
    plugin.getConfig().set("rankeds.profiles." + activeProfileName + ".min", rankedMinSize);
    save();
    load();
  }

  public int getRankedMaxSize() {
    return activeProfile != null ? activeProfile.getMax() : 6;
  }

  public void setRankedMaxSize(int rankedMaxSize) {
    plugin.getConfig().set("rankeds.profiles." + activeProfileName + ".max", rankedMaxSize);
    save();
    load();
  }

  public String getRankedOrder() {
    return activeProfile != null ? activeProfile.getOrder() : "ABBAAB";
  }

  public void setRankedOrder(String rankedOrder) {
    plugin.getConfig().set("rankeds.profiles." + activeProfileName + ".order", rankedOrder);
    save();
    load();
  }

  public String getMapPool() {
    return activeProfile != null ? activeProfile.getMapPool() : "default";
  }

  public String getTable() {
    return activeProfile != null ? activeProfile.getTable() : "";
  }

  public List<String> getRankedMaps() {
    if (activeProfile == null) {
      return new ArrayList<>();
    }

    String poolName = activeProfile.getMapPool();
    if (poolName == null || poolName.isEmpty()) {
      return new ArrayList<>();
    }

    if (PGM.get().getMapOrder() instanceof MapPoolManager) {
      MapPoolManager mapPoolManager = (MapPoolManager) PGM.get().getMapOrder();
      try {
        var pool = mapPoolManager.getMapPools().stream()
            .filter(p -> p.getName().equalsIgnoreCase(poolName))
            .findFirst()
            .orElse(null);

        if (pool != null) {
          return pool.getMaps().stream().map(MapInfo::getName).collect(Collectors.toList());
        }
      } catch (Exception e) {
        plugin
            .getLogger()
            .warning("Error getting maps from pool '" + poolName + "': " + e.getMessage());
      }
    }

    return new ArrayList<>();
  }

  public boolean isMapRanked(String mapName) {
    return getRankedMaps().stream().anyMatch(map -> map.equalsIgnoreCase(mapName));
  }

  public List<String> getMapsFromPool(String poolName) {
    if (poolName == null || poolName.isEmpty()) {
      return new ArrayList<>();
    }

    if (PGM.get().getMapOrder() instanceof MapPoolManager) {
      MapPoolManager mapPoolManager = (MapPoolManager) PGM.get().getMapOrder();
      try {
        var pool = mapPoolManager.getMapPools().stream()
            .filter(p -> p.getName().equalsIgnoreCase(poolName))
            .findFirst()
            .orElse(null);

        if (pool != null) {
          return pool.getMaps().stream().map(MapInfo::getName).collect(Collectors.toList());
        }
      } catch (Exception e) {
        plugin
            .getLogger()
            .warning("Error getting maps from pool '" + poolName + "': " + e.getMessage());
      }
    }

    return new ArrayList<>();
  }

  public boolean setPool(String poolName) {
    if (activeProfile == null) {
      return false;
    }

    if (PGM.get().getMapOrder() instanceof MapPoolManager) {
      MapPoolManager mapPoolManager = (MapPoolManager) PGM.get().getMapOrder();
      boolean poolExists = mapPoolManager.getMapPools().stream()
          .anyMatch(p -> p.getName().equalsIgnoreCase(poolName));

      if (!poolExists) {
        plugin.getLogger().warning("Map pool '" + poolName + "' not found in PGM");
        return false;
      }
    }

    plugin.getConfig().set("rankeds.profiles." + activeProfileName + ".mapPool", poolName);
    save();

    activeProfile.setMapPool(poolName);

    return true;
  }

  public boolean isRankedMatchmaking() {
    return activeProfile != null ? activeProfile.isMatchmaking() : false;
  }

  public void setRankedMatchmaking(boolean rankedMatchmaking) {
    plugin
        .getConfig()
        .set("rankeds.profiles." + activeProfileName + ".matchmaking", rankedMatchmaking);
    save();
    load();
  }

  public int getOnTimerWaiting() {
    return activeProfile != null ? activeProfile.getTimerWaiting() : 30;
  }

  public void setOnTimerWaiting(int onTimerWaiting) {
    plugin
        .getConfig()
        .set("rankeds.profiles." + activeProfileName + ".timer.waiting", onTimerWaiting);
    save();
    load();
  }

  public int getOnTimerMinReached() {
    return activeProfile != null ? activeProfile.getTimerMinReached() : 15;
  }

  public void setOnTimerMinReached(int onTimerMinReached) {
    plugin
        .getConfig()
        .set("rankeds.profiles." + activeProfileName + ".timer.minReached", onTimerMinReached);
    save();
    load();
  }

  public int getOnTimerFull() {
    return activeProfile != null ? activeProfile.getTimerFull() : 5;
  }

  public void setOnTimerFull(int onTimerFull) {
    plugin.getConfig().set("rankeds.profiles." + activeProfileName + ".timer.full", onTimerFull);
    save();
    load();
  }

  public boolean isReroll() {
    return activeProfile != null ? activeProfile.isReroll() : false;
  }

  public void setReroll(boolean reroll) {
    plugin.getConfig().set("rankeds.profiles." + activeProfileName + ".reroll", reroll);
    save();
    load();
  }

  @Override
  public String toString() {
    return "RankedConfig{"
        + "activeProfile='" + activeProfileName + '\''
        + ", profiles=" + profiles.keySet()
        + '}';
  }
}
