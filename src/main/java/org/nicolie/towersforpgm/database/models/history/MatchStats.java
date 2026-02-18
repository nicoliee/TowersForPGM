package org.nicolie.towersforpgm.database.models.history;

import java.time.Duration;
import me.tbg.match.bot.stats.Stats;

public class MatchStats {
  private final String displayName;

  // K/D stats
  private final int kills;
  private final int deaths;
  private final int assists;
  private final int killstreak;
  private final int maxKillstreak;

  // Bow stats
  private final int longestBowKill;
  private final double bowDamage;
  private final double bowDamageTaken;
  private final int shotsTaken;
  private final int shotsHit;

  // Damage stats
  private final double damageDone;
  private final double damageTaken;

  // Objective stats
  private final int destroyablePiecesBroken;
  private final int monumentsDestroyed;
  private final int flagsCaptured;
  private final int flagPickups;
  private final int coresLeaked;
  private final int woolsCaptured;
  private final int woolsTouched;
  private final long longestFlagHoldMillis;
  private final int totalPoints;

  private final String teamName;

  public MatchStats(
      String displayName,
      int kills,
      int deaths,
      int assists,
      int killstreak,
      int maxKillstreak,
      int longestBowKill,
      double bowDamage,
      double bowDamageTaken,
      int shotsTaken,
      int shotsHit,
      double damageDone,
      double damageTaken,
      int destroyablePiecesBroken,
      int monumentsDestroyed,
      int flagsCaptured,
      int flagPickups,
      int coresLeaked,
      int woolsCaptured,
      int woolsTouched,
      long longestFlagHoldMillis,
      int totalPoints,
      String teamName) {
    this.displayName = displayName;
    this.kills = kills;
    this.deaths = deaths;
    this.assists = assists;
    this.killstreak = killstreak;
    this.maxKillstreak = maxKillstreak;
    this.longestBowKill = longestBowKill;
    this.bowDamage = bowDamage;
    this.bowDamageTaken = bowDamageTaken;
    this.shotsTaken = shotsTaken;
    this.shotsHit = shotsHit;
    this.damageDone = damageDone;
    this.damageTaken = damageTaken;
    this.destroyablePiecesBroken = destroyablePiecesBroken;
    this.monumentsDestroyed = monumentsDestroyed;
    this.flagsCaptured = flagsCaptured;
    this.flagPickups = flagPickups;
    this.coresLeaked = coresLeaked;
    this.woolsCaptured = woolsCaptured;
    this.woolsTouched = woolsTouched;
    this.longestFlagHoldMillis = longestFlagHoldMillis;
    this.totalPoints = totalPoints;
    this.teamName = teamName;
  }

  public String getDisplayName() {
    return displayName;
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

  public int getKillstreak() {
    return killstreak;
  }

  public int getMaxKillstreak() {
    return maxKillstreak;
  }

  public int getLongestBowKill() {
    return longestBowKill;
  }

  public double getBowDamage() {
    return bowDamage;
  }

  public double getBowDamageTaken() {
    return bowDamageTaken;
  }

  public int getShotsTaken() {
    return shotsTaken;
  }

  public int getShotsHit() {
    return shotsHit;
  }

  public double getDamageDone() {
    return damageDone;
  }

  public double getDamageTaken() {
    return damageTaken;
  }

  public int getDestroyablePiecesBroken() {
    return destroyablePiecesBroken;
  }

  public int getMonumentsDestroyed() {
    return monumentsDestroyed;
  }

  public int getFlagsCaptured() {
    return flagsCaptured;
  }

  public int getFlagPickups() {
    return flagPickups;
  }

  public int getCoresLeaked() {
    return coresLeaked;
  }

  public int getWoolsCaptured() {
    return woolsCaptured;
  }

  public int getWoolsTouched() {
    return woolsTouched;
  }

  public long getLongestFlagHoldMillis() {
    return longestFlagHoldMillis;
  }

  public int getTotalPoints() {
    return totalPoints;
  }

  public String getTeamName() {
    return teamName;
  }

  public double getKDRatio() {
    if (deaths == 0) {
      return kills > 0 ? kills : 0.0;
    }
    return (double) kills / deaths;
  }

  public double getAccuracy() {
    if (shotsTaken == 0) {
      return 0.0;
    }
    return (double) shotsHit / shotsTaken * 100.0;
  }

  public double getTotalDamageDone() {
    return damageDone + bowDamage;
  }

  public double getTotalDamageTaken() {
    return damageTaken + bowDamageTaken;
  }

  public Stats toStats() {
    return new Stats(
        displayName,
        kills,
        deaths,
        assists,
        killstreak,
        maxKillstreak,
        longestBowKill,
        bowDamage,
        bowDamageTaken,
        shotsTaken,
        shotsHit,
        damageDone,
        damageTaken,
        destroyablePiecesBroken,
        monumentsDestroyed,
        flagsCaptured,
        flagPickups,
        coresLeaked,
        woolsCaptured,
        woolsTouched,
        Duration.ofMillis(longestFlagHoldMillis),
        totalPoints,
        null // party
        );
  }
}
