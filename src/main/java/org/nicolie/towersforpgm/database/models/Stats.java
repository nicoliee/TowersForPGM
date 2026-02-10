package org.nicolie.towersforpgm.database.models;

public class Stats {
  private String username;
  private int kills;
  private int maxKills;
  private int deaths;
  private int assists;
  private double damageDone;
  private double damageTaken;
  private int points;
  private int maxPoints;
  private int wins;
  private int games;
  private int winstreak;
  private int maxWinstreak;
  private int elo;
  private int lastElo;
  private int maxElo;
  private int eloChange;

  public Stats(
      String username,
      int kills,
      int maxKills,
      int deaths,
      int assists,
      double damageDone,
      double damageTaken,
      int points,
      int maxPoints,
      int wins,
      int games,
      int winstreak,
      int maxWinstreak,
      int elo,
      int lastElo,
      int maxElo) {
    this.username = username;
    this.kills = kills;
    this.maxKills = maxKills;
    this.deaths = deaths;
    this.assists = assists;
    this.damageDone = damageDone;
    this.damageTaken = damageTaken;
    this.points = points;
    this.maxPoints = maxPoints;
    this.wins = wins;
    this.games = games;
    this.winstreak = winstreak;
    this.maxWinstreak = maxWinstreak;
    this.elo = elo;
    this.lastElo = lastElo;
    this.maxElo = maxElo;
    this.eloChange = elo - lastElo;
  }

  @Override
  public String toString() {
    return "Stats{" + "username='"
        + username + '\'' + ", kills="
        + kills + ", maxKills="
        + maxKills + ", deaths="
        + deaths + ", assists="
        + assists + ", damageDone="
        + damageDone + ", damageTaken="
        + damageTaken + ", points="
        + points + ", maxPoints="
        + maxPoints + ", wins="
        + wins + ", games="
        + games + ", winstreak="
        + winstreak + ", maxWinstreak="
        + maxWinstreak + ", elo="
        + elo + ", lastElo="
        + lastElo + ", maxElo="
        + maxElo + ", eloChange="
        + eloChange + '}';
  }

  // Getters
  public String getUsername() {
    return username;
  }

  public int getKills() {
    return kills;
  }

  public int getMaxKills() {
    return maxKills;
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

  public int getMaxPoints() {
    return maxPoints;
  }

  public int getWins() {
    return wins;
  }

  public int getGames() {
    return games;
  }

  public int getWinstreak() {
    return winstreak;
  }

  public int getMaxWinstreak() {
    return maxWinstreak;
  }

  public int getElo() {
    return elo;
  }

  public int getLastElo() {
    return lastElo;
  }

  public int getMaxElo() {
    return maxElo;
  }

  public int getEloChange() {
    return eloChange;
  }

  // Métodos calculados adicionales
  public double getKDRatio() {
    if (deaths == 0) {
      return kills > 0 ? kills : 0.0;
    } else {
      return (double) kills / deaths;
    }
  }
}
