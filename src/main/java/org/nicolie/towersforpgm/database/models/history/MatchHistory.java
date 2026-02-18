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
  private final long finishedAt;
  // Nuevo campo - nullable para retrocompatibilidad
  private final List<TeamInfo> teams;

  /** Constructor retrocompatible - mantiene la firma original. */
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
    this(
        matchId,
        tableName,
        mapName,
        durationSeconds,
        ranked,
        scoresText,
        winnersText,
        players,
        finishedAt,
        null);
  }

  /** Constructor completo con información de teams. */
  public MatchHistory(
      String matchId,
      String tableName,
      String mapName,
      int durationSeconds,
      boolean ranked,
      String scoresText,
      String winnersText,
      List<PlayerHistory> players,
      long finishedAt,
      List<TeamInfo> teams) {
    this.matchId = matchId;
    this.tableName = tableName;
    this.mapName = mapName;
    this.durationSeconds = durationSeconds;
    this.ranked = ranked;
    this.scoresText = scoresText;
    this.winnersText = winnersText;
    this.players = players;
    this.finishedAt = finishedAt;
    this.teams = teams;
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

  public List<TeamInfo> getTeams() {
    return teams;
  }

  /** Obtiene los jugadores de un team específico por nombre. */
  public List<PlayerHistory> getPlayersByTeam(String teamName) {
    if (players == null || teamName == null) {
      return List.of();
    }
    return players.stream().filter(p -> teamName.equals(p.getTeamName())).toList();
  }

  /** Obtiene los jugadores ganadores. */
  public List<PlayerHistory> getWinners() {
    if (players == null) {
      return List.of();
    }
    return players.stream().filter(p -> p.getWin() == 1).toList();
  }

  /** Obtiene los jugadores perdedores. */
  public List<PlayerHistory> getLosers() {
    if (players == null) {
      return List.of();
    }
    return players.stream().filter(p -> p.getWin() == 0).toList();
  }
}
