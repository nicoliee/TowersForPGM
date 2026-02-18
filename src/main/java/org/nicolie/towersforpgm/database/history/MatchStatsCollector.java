package org.nicolie.towersforpgm.database.history;

import org.nicolie.towersforpgm.database.models.history.MatchStats;
import tc.oc.pgm.stats.PlayerStats;

public class MatchStatsCollector {

  public static MatchStats createMatchStats(
      PlayerStats playerStats, String displayName, int totalPoints, String teamName) {

    if (playerStats == null) {
      // Retornar estadísticas vacías si no hay PlayerStats disponible
      return new MatchStats(
          displayName,
          0, // kills
          0, // deaths
          0, // assists
          0, // killstreak
          0, // maxKillstreak
          0, // longestBowKill
          0.0, // bowDamage
          0.0, // bowDamageTaken
          0, // shotsTaken
          0, // shotsHit
          0.0, // damageDone
          0.0, // damageTaken
          0, // destroyablePiecesBroken
          0, // monumentsDestroyed
          0, // flagsCaptured
          0, // flagPickups
          0, // coresLeaked
          0, // woolsCaptured
          0, // woolsTouched
          0L, // longestFlagHoldMillis
          totalPoints,
          teamName);
    }

    // K/D stats
    int kills = playerStats.getKills();
    int deaths = playerStats.getDeaths();
    int assists = playerStats.getAssists();
    int killstreak = playerStats.getKillstreak();
    int maxKillstreak = playerStats.getMaxKillstreak();

    // Bow stats
    int longestBowKill = playerStats.getLongestBowKill();
    double bowDamage = playerStats.getBowDamage() / 2.0; // Convertir a corazones
    double bowDamageTaken = playerStats.getBowDamageTaken() / 2.0;
    int shotsTaken = playerStats.getShotsTaken();
    int shotsHit = playerStats.getShotsHit();

    // Damage stats
    double damageDone = playerStats.getDamageDone() / 2.0; // Convertir a corazones
    double damageTaken = playerStats.getDamageTaken() / 2.0;

    // Objective stats
    int destroyablePiecesBroken = playerStats.getDestroyablePiecesBroken();
    int monumentsDestroyed = playerStats.getMonumentsDestroyed();
    int flagsCaptured = playerStats.getFlagsCaptured();
    int flagPickups = playerStats.getFlagPickups();
    int coresLeaked = playerStats.getCoresLeaked();
    int woolsCaptured = playerStats.getWoolsCaptured();
    int woolsTouched = playerStats.getWoolsTouched();
    long longestFlagHoldMillis = playerStats.getLongestFlagHold().toMillis();

    return new MatchStats(
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
        longestFlagHoldMillis,
        totalPoints,
        teamName);
  }
}
