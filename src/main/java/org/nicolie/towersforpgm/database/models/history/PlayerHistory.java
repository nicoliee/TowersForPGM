package org.nicolie.towersforpgm.database.models.history;

public class PlayerHistory {
  private final String username;
  private final int kills;
  private final int deaths;
  private final int assists;
  private final double damageDone;
  private final double damageTaken;
  private final int points;
  private final int win;
  private final int game;
  private final int winstreakDelta;
  private final Integer eloDelta;
  private final Integer maxEloAfter;
  // Nuevos campos - nullable para retrocompatibilidad
  private final String teamName;
  private final String teamColorHex;
  private final Integer eloBefore;
  private final MatchStats matchStats;

  /** Constructor retrocompatible - mantiene la firma original. */
  public PlayerHistory(
      String username,
      int kills,
      int deaths,
      int assists,
      double damageDone,
      double damageTaken,
      int points,
      int win,
      int game,
      int winstreakDelta,
      Integer eloDelta,
      Integer maxEloAfter) {
    this(
        username,
        kills,
        deaths,
        assists,
        damageDone,
        damageTaken,
        points,
        win,
        game,
        winstreakDelta,
        eloDelta,
        maxEloAfter,
        null,
        null,
        null,
        null);
  }

  /** Constructor completo con toda la información nueva. */
  public PlayerHistory(
      String username,
      int kills,
      int deaths,
      int assists,
      double damageDone,
      double damageTaken,
      int points,
      int win,
      int game,
      int winstreakDelta,
      Integer eloDelta,
      Integer maxEloAfter,
      String teamName,
      String teamColorHex,
      Integer eloBefore,
      MatchStats matchStats) {
    this.username = username;
    this.kills = kills;
    this.deaths = deaths;
    this.assists = assists;
    this.damageDone = damageDone;
    this.damageTaken = damageTaken;
    this.points = points;
    this.win = win;
    this.game = game;
    this.winstreakDelta = winstreakDelta;
    this.eloDelta = eloDelta;
    this.maxEloAfter = maxEloAfter;
    this.teamName = teamName;
    this.teamColorHex = teamColorHex;
    this.eloBefore = eloBefore;
    this.matchStats = matchStats;
  }

  public String getUsername() {
    return username;
  }

  public int getKills() {
    return kills;
  }

  public int getDeaths() {
    return deaths;
  }

  public int getAssists() {
    return assists;
  }

  public double getDamageDone() {
    return damageDone;
  }

  public double getDamageTaken() {
    return damageTaken;
  }

  public int getPoints() {
    return points;
  }

  public int getWin() {
    return win;
  }

  public int getGame() {
    return game;
  }

  public int getWinstreakDelta() {
    return winstreakDelta;
  }

  public Integer getEloDelta() {
    return eloDelta;
  }

  public Integer getMaxEloAfter() {
    return maxEloAfter;
  }

  public String getTeamName() {
    return teamName;
  }

  /**
   * Devuelve el color del team en formato hexadecimal (#RRGGBB). Retorna null si no hay información
   * de color.
   */
  public String getTeamColorHex() {
    return teamColorHex;
  }

  public Integer getEloBefore() {
    return eloBefore;
  }

  /**
   * Calcula el elo después sumando el delta al elo anterior. Compatible con maxEloAfter cuando está
   * disponible.
   */
  public Integer getEloAfter() {
    if (maxEloAfter != null) {
      return maxEloAfter;
    }
    if (eloBefore != null && eloDelta != null) {
      return eloBefore + eloDelta;
    }
    return null;
  }

  public MatchStats getMatchStats() {
    return matchStats;
  }
}
