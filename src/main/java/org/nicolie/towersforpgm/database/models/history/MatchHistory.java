package org.nicolie.towersforpgm.database.models.history;

import java.util.List;

public class MatchHistory {
  private final String matchId;
  private final String tableName;
  private final String mapName;
  private final int durationSeconds;
  private final boolean ranked;
  private final String scoresText;
  private final String winnersText;
  private final List<PlayerHistory> players;
  private final long finishedAt; // Timestamp en epoch seconds

  public MatchHistory(
      String matchId,
      String tableName,
      String mapName,
      int durationSeconds,
      boolean ranked,
      String scoresText,
      String winnersText,
      List<PlayerHistory> players,
      long finishedAt) {
    this.matchId = matchId;
    this.tableName = tableName;
    this.mapName = mapName;
    this.durationSeconds = durationSeconds;
    this.ranked = ranked;
    this.scoresText = scoresText;
    this.winnersText = winnersText;
    this.players = players;
    this.finishedAt = finishedAt;
  }

  public String getMatchId() {
    return matchId;
  }

  public String getTableName() {
    return tableName;
  }

  public String getMapName() {
    return mapName;
  }

  public int getDurationSeconds() {
    return durationSeconds;
  }

  public boolean isRanked() {
    return ranked;
  }

  public String getScoresText() {
    return scoresText;
  }

  public String getWinnersText() {
    return winnersText;
  }

  public List<PlayerHistory> getPlayers() {
    return players;
  }

  public long getFinishedAt() {
    return finishedAt;
  }
}
