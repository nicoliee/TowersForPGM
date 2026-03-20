package org.nicolie.towersforpgm.commands.history.gui.match;

import net.kyori.adventure.text.Component;
import org.nicolie.towersforpgm.database.models.history.MatchStats;
import org.nicolie.towersforpgm.database.models.history.PlayerHistory;

public enum MatchSortOrder {
  NAME,
  KILLS,
  DEATHS,
  KDR,
  ASSISTS,
  DAMAGE_DONE,
  DAMAGE_TAKEN,
  BOW_ACCURACY,
  POINTS;

  public MatchSortOrder next() {
    MatchSortOrder[] vals = MatchSortOrder.values();
    return vals[(this.ordinal() + 1) % vals.length];
  }

  public MatchSortOrder previous() {
    MatchSortOrder[] vals = MatchSortOrder.values();
    return vals[(this.ordinal() - 1 + vals.length) % vals.length];
  }

  public Component label() {
    switch (this) {
      case NAME:
        return Component.translatable("stats.name");
      case KILLS:
        return Component.translatable("stats.kills");
      case DEATHS:
        return Component.translatable("stats.deaths");
      case KDR:
        return Component.translatable("stats.kdratio");
      case ASSISTS:
        return Component.translatable("stats.assists");
      case DAMAGE_DONE:
        return Component.translatable("stats.damageDone");
      case DAMAGE_TAKEN:
        return Component.translatable("stats.damageTaken");
      case BOW_ACCURACY:
        return Component.translatable("stats.bow");
      case POINTS:
        return Component.translatable("stats.points");
      default:
        return Component.text(name());
    }
  }

  public double getValue(PlayerHistory ph) {
    MatchStats ms = ph.getMatchStats();
    switch (this) {
      case KILLS:
        return ms != null ? ms.getKills() : ph.getKills();
      case DEATHS:
        return ms != null ? ms.getDeaths() : ph.getDeaths();
      case KDR: {
        int k = ms != null ? ms.getKills() : ph.getKills();
        int d = ms != null ? ms.getDeaths() : ph.getDeaths();
        return d > 0 ? (double) k / d : k;
      }
      case ASSISTS:
        return ms != null ? ms.getAssists() : ph.getAssists();
      case DAMAGE_DONE:
        return ms != null ? ms.getDamageDone() : ph.getDamageDone();
      case DAMAGE_TAKEN:
        return ms != null ? ms.getDamageTaken() : ph.getDamageTaken();
      case BOW_ACCURACY: {
        if (ms == null || ms.getShotsTaken() == 0) return 0;
        return (double) ms.getShotsHit() / ms.getShotsTaken() * 100;
      }
      case POINTS:
        return ph.getPoints();
      default:
        return 0;
    }
  }
}
