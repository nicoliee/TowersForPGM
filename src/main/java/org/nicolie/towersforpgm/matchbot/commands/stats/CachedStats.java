package org.nicolie.towersforpgm.matchbot.commands.stats;

import java.io.File;
import org.nicolie.towersforpgm.database.models.Stats;

public class CachedStats {

  private final String table;
  private final Stats stats;
  private final Object eloHistory;
  private final File chartFile;
  private long timestamp;

  public CachedStats(String table, Stats stats, Object eloHistory, File chartFile) {
    this.table = table;
    this.stats = stats;
    this.eloHistory = eloHistory;
    this.chartFile = chartFile;
    this.timestamp = System.currentTimeMillis();
  }

  public boolean isExpired(long ttl) {
    return System.currentTimeMillis() - timestamp > ttl;
  }

  public void refresh() {
    this.timestamp = System.currentTimeMillis();
  }

  public String getTable() {
    return table;
  }

  public Stats getStats() {
    return stats;
  }

  public Object getEloHistory() {
    return eloHistory;
  }

  public File getChartFile() {
    return chartFile;
  }
}
