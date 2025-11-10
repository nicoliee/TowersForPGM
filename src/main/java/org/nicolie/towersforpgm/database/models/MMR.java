package org.nicolie.towersforpgm.database.models;

public final class MMR {

  private MMR() {}

  public static double compute(Stats stats) {
    if (stats == null) return 0.0;

    // Base: prefer Elo when present; treat -9999 (unranked) and negatives as 0
    double baseElo = stats.getElo();
    if (baseElo == -9999) baseElo = 0;

    // Rates per game to avoid bias by volume
    double games = Math.max(1, stats.getGames());
    double killsPerGame = stats.getKills() / games;
    double deathsPerGame = stats.getDeaths() / games;
    double assistsPerGame = stats.getAssists() / games;
    double pointsPerGame = stats.getPoints() / games;
    double damageDonePerGame = stats.getDamageDone() / games;
    double damageTakenPerGame = stats.getDamageTaken() / games;
    double winRate = stats.getGames() > 0 ? (stats.getWins() / games) : 0.0;

    // Quick role inference to weight features adaptively
    double attackSignal = pointsPerGame * 1.5 + damageDonePerGame * 0.2 + killsPerGame * 0.8;
    double defendSignal =
        damageTakenPerGame * 0.6 + (1.0 / Math.max(0.1, deathsPerGame)) * 0.5 + winRate * 1.0;

    Role role;
    if (attackSignal > defendSignal * 1.15) {
      role = Role.ATTACKER;
    } else if (defendSignal > attackSignal * 1.15) {
      role = Role.DEFENDER;
    } else {
      role = Role.HYBRID;
    }

    double performance = 0.0;
    switch (role) {
      case ATTACKER:
        performance += pointsPerGame * 4.0;
        performance += killsPerGame * 3.0;
        performance += assistsPerGame * 1.5;
        performance += winRate * 40.0;
        performance += damageDonePerGame * 0.1;
        performance -= deathsPerGame * 1.5;
        break;
      case DEFENDER:
        performance += damageTakenPerGame * 0.8;
        performance += assistsPerGame * 2.0;
        performance += (1.0 / Math.max(0.1, deathsPerGame)) * 2.0;
        performance += winRate * 30.0;
        performance += killsPerGame * 1.0;
        break;
      case HYBRID:
      default:
        performance += pointsPerGame * 2.5;
        performance += killsPerGame * 2.0;
        performance += assistsPerGame * 1.8;
        performance += winRate * 35.0;
        performance += (damageDonePerGame + damageTakenPerGame) * 0.15;
        performance -= deathsPerGame * 1.0;
        break;
    }

    // Light stability bonus for players with more sample size
    if (stats.getGames() >= 12) {
      // Encourage consistent KDA patterns (bounded effect)
      double stability = 1.0
          - (Math.abs(killsPerGame - deathsPerGame)
                  + Math.abs(assistsPerGame - Math.max(0.1, killsPerGame)))
              / 350.0; // soft scale similar to prior heuristic
      performance += Math.max(0.0, stability) * 1.5;
    }

    // Blend: 60% Elo, 40% performance
    double combined = baseElo * 0.6 + performance * 0.4;

    // Non-negative and finite
    if (Double.isNaN(combined) || Double.isInfinite(combined)) return 0.0;
    return Math.max(0.0, combined);
  }

  public static int computeInt(Stats stats) {
    return (int) Math.round(compute(stats));
  }

  public enum Role {
    ATTACKER,
    DEFENDER,
    HYBRID
  }
}
