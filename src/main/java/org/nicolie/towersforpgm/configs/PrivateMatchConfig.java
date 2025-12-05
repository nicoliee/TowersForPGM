package org.nicolie.towersforpgm.configs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;
import org.nicolie.towersforpgm.TowersForPGM;

public class PrivateMatchConfig {
  private final JavaPlugin plugin;
  private Map<String, Boolean> privateMatchMap = new HashMap<>();

  public PrivateMatchConfig(JavaPlugin plugin) {
    this.plugin = plugin;
    load();
  }

  public void load() {
    privateMatchMap.clear();

    if (plugin.getConfig().contains("privateMatch")) {
      List<String> privateMaps = plugin.getConfig().getStringList("privateMatch");
      if (privateMaps != null) {
        for (String map : privateMaps) {
          if (map != null && !map.isEmpty()) {
            privateMatchMap.put(map, true);
          }
        }
      }
    }
  }

  private void save() {
    plugin.saveConfig();
  }

  public boolean isPrivateMatch(String map) {
    return privateMatchMap.getOrDefault(map, false);
  }

  public void setPrivateMatch(String mapName, boolean privateMatch) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    privateMatchMap.put(mapName, privateMatch);

    List<String> privateMaps = plugin.getConfig().getStringList("database.privateMatch");
    if (privateMaps == null) {
      privateMaps = new ArrayList<>();
    }

    if (privateMatch) {
      if (!privateMaps.contains(mapName)) {
        privateMaps.add(mapName);
      }
    } else {
      privateMaps.remove(mapName);
    }

    plugin.getConfig().set("database.privateMatch", privateMaps);
    save();
  }

  @Override
  public String toString() {
    return "PrivateMatchConfig{" + "privateMatchMap=" + privateMatchMap + '}';
  }
}
