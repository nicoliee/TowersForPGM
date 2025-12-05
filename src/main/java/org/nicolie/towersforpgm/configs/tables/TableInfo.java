package org.nicolie.towersforpgm.configs.tables;

import java.util.ArrayList;
import java.util.List;

public class TableInfo {
  private final List<String> maps;
  private final boolean ranked;

  public TableInfo(List<String> maps, boolean ranked) {
    this.maps = new ArrayList<>(maps);
    this.ranked = ranked;
  }

  public List<String> getMaps() {
    return maps;
  }

  public boolean isRanked() {
    return ranked;
  }

  public boolean addMap(String map) {
    if (!maps.contains(map)) {
      maps.add(map);
      return true;
    }
    return false;
  }

  public boolean removeMap(String map) {
    return maps.remove(map);
  }
}
