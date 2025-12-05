package org.nicolie.towersforpgm.configs;

import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

public class RankedConfig {
  private final JavaPlugin plugin;
  private int disconnectTime;
  private int rankedMinSize;
  private int rankedMaxSize;
  private String rankedOrder;
  private List<String> rankedMaps;
  private boolean rankedMatchmaking;

  public RankedConfig(JavaPlugin plugin) {
    this.plugin = plugin;
    load();
  }

  public void load() {
    disconnectTime = plugin.getConfig().getInt("rankeds.disconnectTime", 60);
    rankedMinSize = plugin.getConfig().getInt("rankeds.size.min", 4);
    rankedMaxSize = plugin.getConfig().getInt("rankeds.size.max", 8);
    rankedOrder = plugin.getConfig().getString("rankeds.order", "");
    rankedMaps = plugin.getConfig().getStringList("rankeds.maps");
    rankedMatchmaking = plugin.getConfig().getBoolean("rankeds.matchmaking", false);
  }

  private void save() {
    plugin.saveConfig();
  }

  public int getDisconnectTime() {
    return disconnectTime;
  }

  public void setDisconnectTime(int disconnectTime) {
    this.disconnectTime = disconnectTime;
    plugin.getConfig().set("rankeds.disconnectTime", disconnectTime);
    save();
  }

  public int getRankedMinSize() {
    return rankedMinSize;
  }

  public void setRankedMinSize(int rankedMinSize) {
    this.rankedMinSize = rankedMinSize;
    plugin.getConfig().set("rankeds.size.min", rankedMinSize);
    save();
  }

  public int getRankedMaxSize() {
    return rankedMaxSize;
  }

  public void setRankedMaxSize(int rankedMaxSize) {
    this.rankedMaxSize = rankedMaxSize;
    plugin.getConfig().set("rankeds.size.max", rankedMaxSize);
    save();
  }

  public String getRankedOrder() {
    return rankedOrder;
  }

  public void setRankedOrder(String rankedOrder) {
    this.rankedOrder = rankedOrder;
    plugin.getConfig().set("rankeds.order", rankedOrder);
    save();
  }

  public List<String> getRankedMaps() {
    return rankedMaps;
  }

  public void addMap(String map) {
    rankedMaps.add(map);
    plugin.getConfig().set("rankeds.maps", rankedMaps);
    save();
  }

  public void removeMap(String map) {
    rankedMaps.remove(map);
    plugin.getConfig().set("rankeds.maps", rankedMaps);
    save();
  }

  public boolean isMapRanked(String map) {
    return rankedMaps.contains(map);
  }

  public void setRankedMatchmaking(boolean rankedMatchmaking) {
    this.rankedMatchmaking = rankedMatchmaking;
    plugin.getConfig().set("rankeds.matchmaking", rankedMatchmaking);
    save();
  }

  public boolean isRankedMatchmaking() {
    return rankedMatchmaking;
  }

  public void setRankedMatchmaking(Boolean rankedMatchmaking) {
    this.rankedMatchmaking = rankedMatchmaking;
    plugin.getConfig().set("rankeds.matchmaking", rankedMatchmaking);
    save();
  }

  @Override
  public String toString() {
    return "RankedConfig{" + "disconnectTime="
        + disconnectTime + ", rankedMinSize="
        + rankedMinSize + ", rankedMaxSize="
        + rankedMaxSize + ", rankedOrder='"
        + rankedOrder + '\'' + ", rankedMaps="
        + rankedMaps + ", rankedMatchmaking="
        + rankedMatchmaking + '}';
  }
}
