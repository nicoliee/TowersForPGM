package org.nicolie.towersforpgm.database;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.history.MatchHistoryService;
import org.nicolie.towersforpgm.database.history.MatchStatsCollector;
import org.nicolie.towersforpgm.database.history.TeamInfoExtractor;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.database.models.history.MatchHistory;
import org.nicolie.towersforpgm.database.models.history.MatchStats;
import org.nicolie.towersforpgm.database.models.history.TeamInfo;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.util.Audience;

public class MatchHistoryManager {

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final String SERVER_ID =
      TowersForPGM.getInstance().config().database().getId();

  private static final ConcurrentHashMap<String, AtomicInteger> counters =
      new ConcurrentHashMap<>();

  private static final Executor DB_EXECUTOR = Executors.newFixedThreadPool(8, r -> {
    Thread t = new Thread(r, "match-db-exec");
    t.setDaemon(true);
    return t;
  });

  private static final MatchHistoryService historyService = new MatchHistoryService();

  public static CompletableFuture<Void> preloadMatchIdCountersAsync(Collection<String> tables) {
    return CompletableFuture.runAsync(
        () -> {
          String datePart = LocalDate.now().format(DATE_FMT);
          counters.keySet().removeIf(k -> !k.endsWith("|" + datePart));
          for (String table : tables) {
            try {
              int count = historyService.getCountForDay(table, datePart);
              counters.put(table + "|" + datePart, new AtomicInteger(count + 1));
              TowersForPGM.getInstance()
                  .getLogger()
                  .info("MatchHistoryManager: " + table + " -> " + (count + 1));
            } catch (Exception e) {
              TowersForPGM.getInstance()
                  .getLogger()
                  .warning("Error precargando matchId counter para tabla "
                      + table
                      + ": "
                      + e.getMessage());
            }
          }
        },
        DB_EXECUTOR);
  }

  public static CompletableFuture<String> generateMatchId(String table) {
    return CompletableFuture.supplyAsync(
        () -> {
          String datePart = LocalDate.now().format(DATE_FMT);
          String key = table + "|" + datePart;

          counters.keySet().removeIf(k -> !k.endsWith("|" + datePart));

          AtomicInteger counter = counters.computeIfAbsent(key, k -> {
            int nextSeq = getNextSequenceForDay(table, datePart);
            return new AtomicInteger(nextSeq);
          });

          int next = counter.getAndIncrement();
          String matchId = SERVER_ID + "-" + table + "-" + datePart + "-" + next;
          return matchId;
        },
        DB_EXECUTOR);
  }

  private static int getNextSequenceForDay(String table, String datePart) {
    return historyService.getCountForDay(table, datePart) + 1;
  }

  public static CompletableFuture<Void> saveMatch(
      String matchId,
      String table,
      org.nicolie.towersforpgm.matchbot.embeds.MatchInfo matchInfo,
      boolean ranked,
      List<Stats> rawStats,
      List<PlayerEloChange> eloChanges,
      List<TeamInfo> teams,
      Map<String, TeamInfo> playerTeamMap,
      Map<String, MatchStats> playerMatchStats) {
    return historyService.saveMatch(
        matchId,
        table,
        matchInfo,
        ranked,
        rawStats,
        eloChanges,
        teams,
        playerTeamMap,
        playerMatchStats);
  }

  public static CompletableFuture<MatchHistory> getMatch(String matchId) {
    return historyService.getMatch(matchId);
  }

  public static CompletableFuture<List<String>> getRecentMatchIds(String userInput) {
    return historyService.getRecentMatchIds(userInput);
  }

  public static CompletableFuture<Void> rollbackMatch(Audience audience, MatchHistory history) {
    return historyService.rollbackMatch(audience, history);
  }

  public static List<TeamInfo> extractTeamInfo(Match match) {
    return TeamInfoExtractor.extractTeamInfo(match);
  }

  public static MatchStats createMatchStats(
      PlayerStats playerStats, String displayName, int totalPoints, String teamName) {
    return MatchStatsCollector.createMatchStats(playerStats, displayName, totalPoints, teamName);
  }

  public static Map<String, TeamInfo> createPlayerTeamMap(Match match) {
    return TeamInfoExtractor.createPlayerTeamMap(match);
  }

  public static CompletableFuture<List<String>> getPlayerMatchIds(
      String username, String table, int limit) {
    return historyService.getPlayerMatchIds(username, table, limit);
  }

  public static CompletableFuture<List<MatchHistory>> getPlayerMatchHistory(
      String username, String table, int limit) {
    return historyService.getPlayerMatchHistory(username, table, limit);
  }
}
