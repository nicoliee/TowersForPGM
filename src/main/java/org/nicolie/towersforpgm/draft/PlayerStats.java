package org.nicolie.towersforpgm.draft;

public class PlayerStats {
  private int elo;
  private int kills;
  private int deaths;
  private int assists;
  private double damageDone;
  private double damageTaken;
  private int points;
  private int wins;
  private int games;

  public PlayerStats(
      int elo,
      int kills,
      int deaths,
      int assists,
      double damageDone,
      double damageTaken,
      int points,
      int wins,
      int games) {
    this.elo = elo;
    this.kills = kills;
    this.deaths = deaths;
    this.assists = assists;
    this.damageDone = damageDone;
    this.damageTaken = damageTaken;
    this.points = points;
    this.wins = wins;
    this.games = games;
  }

  public int getElo() {
    return elo;
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

  public int getWins() {
    return wins;
  }

  public int getGames() {
    return games;
  }
}
