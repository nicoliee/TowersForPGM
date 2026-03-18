package org.nicolie.towersforpgm.draft.pick.gui;

import net.kyori.adventure.text.Component;
import org.nicolie.towersforpgm.database.models.Stats;

public enum SortOrder {
  NAME,
  KILLS,
  POINTS,
  WINS;

  public SortOrder next() {
    SortOrder[] vals = SortOrder.values();
    return vals[(this.ordinal() + 1) % vals.length];
  }

  public Component label() {
    switch (this) {
      case NAME:
        return Component.translatable("draft.gui.sort.name");
      case KILLS:
        return Component.translatable("stats.kills");
      case POINTS:
        return Component.translatable("stats.points");
      case WINS:
        return Component.translatable("stats.wins");
      default:
        return Component.translatable("draft.gui.sort.name");
    }
  }

  public int statValue(Stats stats) {
    switch (this) {
      case KILLS:
        return stats.getKills();
      case POINTS:
        return stats.getPoints();
      case WINS:
        return stats.getWins();
      default:
        return 0;
    }
  }
}
