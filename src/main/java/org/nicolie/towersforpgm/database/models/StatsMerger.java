package org.nicolie.towersforpgm.database.models;

public class StatsMerger {

  public static Stats merge(Stats oldStats, Stats newStats) {
    if (oldStats == null) {
      return newStats;
    }
    if (newStats == null) {
      return oldStats;
    }

    int totalKills = oldStats.getKills() + newStats.getKills();
    int totalDeaths = oldStats.getDeaths() + newStats.getDeaths();
    int totalAssists = oldStats.getAssists() + newStats.getAssists();
    double totalDamageDone = oldStats.getDamageDone() + newStats.getDamageDone();
    double totalDamageTaken = oldStats.getDamageTaken() + newStats.getDamageTaken();
    int totalPoints = oldStats.getPoints() + newStats.getPoints();
    int totalWins = oldStats.getWins() + newStats.getWins();
    int totalGames = oldStats.getGames() + newStats.getGames();

    int maxKills = Math.max(oldStats.getMaxKills(), newStats.getMaxKills());
    int maxPoints = Math.max(oldStats.getMaxPoints(), newStats.getMaxPoints());
    int maxElo = Math.max(oldStats.getMaxElo(), newStats.getMaxElo());
    int maxWinstreak = Math.max(oldStats.getMaxWinstreak(), newStats.getMaxWinstreak());

    int currentElo;
    int lastElo;
    if (oldStats.getElo() > newStats.getElo()) {
      currentElo = oldStats.getElo();
      lastElo = oldStats.getLastElo();
    } else {
      currentElo = newStats.getElo();
      lastElo = newStats.getLastElo();
    }

    int currentWinstreak = newStats.getWinstreak();

    return new Stats(
        newStats.getUsername(), // Usar el nombre de la cuenta destino
        totalKills,
        maxKills,
        totalDeaths,
        totalAssists,
        totalDamageDone,
        totalDamageTaken,
        totalPoints,
        maxPoints,
        totalWins,
        totalGames,
        currentWinstreak,
        maxWinstreak,
        currentElo,
        lastElo,
        maxElo);
  }

  public static Stats createFromOld(Stats oldStats, String newUsername) {
    if (oldStats == null) {
      return null;
    }

    return new Stats(
        newUsername,
        oldStats.getKills(),
        oldStats.getMaxKills(),
        oldStats.getDeaths(),
        oldStats.getAssists(),
        oldStats.getDamageDone(),
        oldStats.getDamageTaken(),
        oldStats.getPoints(),
        oldStats.getMaxPoints(),
        oldStats.getWins(),
        oldStats.getGames(),
        0, // La nueva cuenta empieza sin winstreak actual
        oldStats.getMaxWinstreak(),
        oldStats.getElo(),
        oldStats.getLastElo(),
        oldStats.getMaxElo());
  }
}
