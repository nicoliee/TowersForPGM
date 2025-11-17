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
}
