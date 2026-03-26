package org.nicolie.towersforpgm.draft.map.modes;

import java.util.List;
import java.util.Random;

public final class AutomaticMode {

  private final List<String> maps;

  public AutomaticMode(List<String> maps) {
    this.maps = maps;
  }

  public String resolveWinner() {
    if (maps == null || maps.isEmpty()) return null;
    return maps.get(new Random().nextInt(maps.size()));
  }
}
