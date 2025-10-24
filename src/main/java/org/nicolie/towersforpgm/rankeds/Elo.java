package org.nicolie.towersforpgm.rankeds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.utils.ConfigManager;
import tc.oc.pgm.api.player.MatchPlayer;

public class Elo {
  private static final int K_FACTOR = 32;

  public static CompletableFuture<List<PlayerEloChange>> addWin(
      List<MatchPlayer> winners, List<MatchPlayer> losers) {
    List<MatchPlayer> allPlayers = new ArrayList<>();
    allPlayers.addAll(winners);
    allPlayers.addAll(losers);
    List<String> usernames =
        allPlayers.stream().map(MatchPlayer::getNameLegacy).collect(Collectors.toList());

    String table = ConfigManager.getRankedDefaultTable();
    if (table == null || table.isEmpty()) {
      org.bukkit.Bukkit.getLogger().warning("[Elo.addWin] Table is null or empty");
      CompletableFuture<List<PlayerEloChange>> future = new CompletableFuture<>();
      future.completeExceptionally(new RuntimeException("Table is null or empty"));
      return future;
    }

    return StatsManager.getEloForUsernames(table, usernames).thenApply(eloList -> {
      List<PlayerEloChange> changes = new ArrayList<>();

      Map<String, PlayerEloChange> eloMap =
          eloList.stream().collect(Collectors.toMap(PlayerEloChange::getUsername, e -> e));
      Map<String, Integer> currentEloMap = eloMap.entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getCurrentElo()));

      if (losers.isEmpty()) {
        winners.forEach(player -> {
          String username = player.getNameLegacy();
          PlayerEloChange playerElo =
              eloMap.getOrDefault(username, new PlayerEloChange(username, 0, 0, 0, 0));
          int currentElo = playerElo.getCurrentElo();
          int maxElo = playerElo.getMaxElo();
          changes.add(new PlayerEloChange(username, currentElo, currentElo, 0, maxElo));
        });
        return changes;
      }

      double winnersAvgElo = calculateTeamAverageElo(winners, currentEloMap);
      double losersAvgElo = calculateTeamAverageElo(losers, currentEloMap);

      winners.forEach(winner -> {
        String username = winner.getNameLegacy();
        PlayerEloChange playerElo =
            eloMap.getOrDefault(username, new PlayerEloChange(username, 0, 0, 0, 0));
        int currentElo = playerElo.getCurrentElo();

        int individualEloChange =
            calculateIndividualEloChange(currentElo, winnersAvgElo, losersAvgElo, true);

        int newElo = currentElo + individualEloChange;
        int maxElo = Math.max(newElo, playerElo.getMaxElo());
        changes.add(new PlayerEloChange(username, currentElo, newElo, individualEloChange, maxElo));
      });

      losers.forEach(loser -> {
        String username = loser.getNameLegacy();
        PlayerEloChange playerElo =
            eloMap.getOrDefault(username, new PlayerEloChange(username, 0, 0, 0, 0));
        int currentElo = playerElo.getCurrentElo();

        int individualEloChange =
            calculateIndividualEloChange(currentElo, losersAvgElo, winnersAvgElo, false);

        int newElo = Math.max(-100, currentElo + individualEloChange);
        int maxElo = playerElo.getMaxElo();
        changes.add(new PlayerEloChange(username, currentElo, newElo, individualEloChange, maxElo));
      });
      return changes;
    });
  }

  public static CompletableFuture<EloOrderResult> eloOrder(List<MatchPlayer> players) {
    List<String> usernames =
        players.stream().map(MatchPlayer::getNameLegacy).collect(Collectors.toList());
    String table = ConfigManager.getRankedDefaultTable();

    if (table == null || table.isEmpty()) {
      CompletableFuture<EloOrderResult> future = new CompletableFuture<>();
      future.complete(new EloOrderResult(null, 0));
      return future;
    }

    return StatsManager.getEloForUsernames(table, usernames).thenApply(eloList -> {
      List<PlayerEloChange> sortedList = eloList.stream()
          .sorted((e1, e2) -> Integer.compare(e2.getCurrentElo(), e1.getCurrentElo()))
          .collect(Collectors.toList());

      int avgElo = 0;
      if (!sortedList.isEmpty()) {
        int total = sortedList.stream().mapToInt(PlayerEloChange::getCurrentElo).sum();
        avgElo = total / sortedList.size();
      }
      return new EloOrderResult(sortedList, avgElo);
    });
  }

  public static class EloOrderResult {
    private final List<PlayerEloChange> sortedPlayers;
    private final int averageElo;

    public EloOrderResult(List<PlayerEloChange> sortedPlayers, int averageElo) {
      this.sortedPlayers = sortedPlayers;
      this.averageElo = averageElo;
    }

    public List<PlayerEloChange> getSortedPlayers() {
      return sortedPlayers;
    }

    public int getAverageElo() {
      return averageElo;
    }
  }

  private static double calculateTeamAverageElo(
      List<MatchPlayer> team, Map<String, Integer> eloMap) {
    if (team.isEmpty()) return 0.0;

    double totalElo = 0.0;
    for (MatchPlayer player : team) {
      totalElo += eloMap.getOrDefault(player.getNameLegacy(), 0);
    }
    return totalElo / team.size();
  }

  private static int calculateIndividualEloChange(
      int playerElo, double teamAvgElo, double opponentAvgElo, boolean isWinner) {
    double playerExpectedScore = 1.0 / (1.0 + Math.pow(10.0, (opponentAvgElo - playerElo) / 400.0));
    double actualScore = isWinner ? 1.0 : 0.0;

    int baseEloChange = (int) Math.round(K_FACTOR * (actualScore - playerExpectedScore));

    double skillDifference = playerElo - teamAvgElo;
    double individualScalingFactor;

    if (isWinner) {
      individualScalingFactor = 1.0 - (skillDifference / 800.0);
      individualScalingFactor = Math.max(0.7, Math.min(1.3, individualScalingFactor));
    } else {
      individualScalingFactor = 1.0 + (skillDifference / 800.0);
      individualScalingFactor = Math.max(0.7, Math.min(1.3, individualScalingFactor));
    }

    double teamStrengthDifference = teamAvgElo - opponentAvgElo;
    double teamBalanceScalingFactor;

    if (isWinner) {
      teamBalanceScalingFactor = 1.0 - (teamStrengthDifference / 600.0);
      teamBalanceScalingFactor = Math.max(0.5, Math.min(1.5, teamBalanceScalingFactor));
    } else {
      teamBalanceScalingFactor = 1.0 + (teamStrengthDifference / 600.0);
      teamBalanceScalingFactor = Math.max(0.5, Math.min(1.5, teamBalanceScalingFactor));
    }

    double combinedScalingFactor = individualScalingFactor * teamBalanceScalingFactor;
    int scaledEloChange = (int) Math.round(baseEloChange * combinedScalingFactor);

    int minChange = isWinner ? 3 : -45;
    int maxChange = isWinner ? 45 : -3;

    return Math.max(minChange, Math.min(maxChange, scaledEloChange));
  }
}
