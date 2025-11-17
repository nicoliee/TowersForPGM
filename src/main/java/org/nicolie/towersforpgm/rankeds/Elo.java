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

      for (MatchPlayer winner : winners) {
        String username = winner.getNameLegacy();
        PlayerEloChange playerElo =
            eloMap.getOrDefault(username, new PlayerEloChange(username, 0, 0, 0, 0));
        int currentElo = playerElo.getCurrentElo();
        Rank rank = Rank.getRankByElo(currentElo);
        int base = rank.getEloWin();
        int jitter = randomJitter();
        int individualEloChange = base + jitter;
        int newElo = currentElo + individualEloChange;
        int maxElo = Math.max(newElo, playerElo.getMaxElo());
        changes.add(new PlayerEloChange(username, currentElo, newElo, individualEloChange, maxElo));
      }

      for (MatchPlayer loser : losers) {
        String username = loser.getNameLegacy();
        PlayerEloChange playerElo =
            eloMap.getOrDefault(username, new PlayerEloChange(username, 0, 0, 0, 0));
        int currentElo = playerElo.getCurrentElo();
        Rank rank = Rank.getRankByElo(currentElo);
        int base = rank.getEloLose(); // negative
        int jitter = randomJitter();
        int individualEloChange = base + jitter;
        int newElo = Math.max(-100, currentElo + individualEloChange);
        int maxElo = playerElo.getMaxElo();
        changes.add(new PlayerEloChange(username, currentElo, newElo, individualEloChange, maxElo));
      }

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

  public static CompletableFuture<PlayerEloChange> doubleLossPenalty(
      String sanctionedUsername, List<MatchPlayer> sanctionedTeam, List<MatchPlayer> opponent) {
    String table = ConfigManager.getRankedDefaultTable();
    if (table == null || table.isEmpty()) {
      CompletableFuture<PlayerEloChange> f = new CompletableFuture<>();
      f.complete(new PlayerEloChange(sanctionedUsername, 0, 0, 0, 0));
      return f;
    }

    List<String> usernames = java.util.Collections.singletonList(sanctionedUsername);
    return StatsManager.getEloForUsernames(table, usernames).thenApply(eloList -> {
      PlayerEloChange base = eloList.stream()
          .filter(e -> e.getUsername().equals(sanctionedUsername))
          .findFirst()
          .orElse(new PlayerEloChange(sanctionedUsername, 0, 0, 0, 0));
      int current = base.getCurrentElo();
      Rank rank = Rank.getRankByElo(current);
      int baseLose = rank.getEloLose(); // negative
      int jitter = randomJitter();
      int penalty = (baseLose + jitter) * 2; // double the loss
      int newElo = Math.max(-100, current + penalty);
      return new PlayerEloChange(sanctionedUsername, current, newElo, penalty, base.getMaxElo());
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

  public static int randomJitter() {
    // +/- 1..3
    int magnitude = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 4);
    boolean positive = java.util.concurrent.ThreadLocalRandom.current().nextBoolean();
    return positive ? magnitude : -magnitude;
  }
}
