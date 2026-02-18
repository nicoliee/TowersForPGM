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

/**
 * Gestor principal del historial de partidas. Coordina la generación de IDs únicos y delega las
 * operaciones a los servicios especializados.
 */
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

  /**
   * Precarga los contadores de matchId para las tablas especificadas.
   *
   * @param tables Colección de nombres de tablas
   * @return CompletableFuture que se completa cuando termina la precarga
   */
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

  /**
   * Genera un ID único para un match.
   *
   * @param table Nombre de la tabla
   * @return CompletableFuture con el ID generado
   */
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
          return SERVER_ID + "-" + table + "-" + datePart + "-" + next;
        },
        DB_EXECUTOR);
  }

  private static int getNextSequenceForDay(String table, String datePart) {
    return historyService.getCountForDay(table, datePart) + 1;
  }

  /**
   * Guarda un match completo en la base de datos.
   *
   * @param matchId ID del match
   * @param table Nombre de la tabla
   * @param matchInfo Información del match
   * @param ranked Si es ranked
   * @param rawStats Estadísticas básicas de los jugadores
   * @param eloChanges Cambios de ELO (puede ser null)
   * @param teams Información de equipos (puede ser null)
   * @param playerTeamMap Mapa jugador -> equipo (puede ser null)
   * @param playerMatchStats Mapa jugador -> estadísticas detalladas (puede ser null)
   * @return CompletableFuture que se completa cuando termina el guardado
   */
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

  /**
   * Método retrocompatible para guardar un match sin información extendida.
   *
   * @deprecated Use {@link #saveMatch(String, String,
   *     org.nicolie.towersforpgm.matchbot.embeds.MatchInfo, boolean, List, List, List, Map, Map)}
   */
  @Deprecated
  public static CompletableFuture<Void> saveMatch(
      String matchId,
      String table,
      org.nicolie.towersforpgm.matchbot.embeds.MatchInfo matchInfo,
      boolean ranked,
      List<Stats> rawStats,
      List<PlayerEloChange> eloChanges) {
    return saveMatch(matchId, table, matchInfo, ranked, rawStats, eloChanges, null, null, null);
  }

  /**
   * Obtiene un match del historial.
   *
   * @param matchId ID del match
   * @return CompletableFuture con el MatchHistory
   */
  public static CompletableFuture<MatchHistory> getMatch(String matchId) {
    return historyService.getMatch(matchId);
  }

  /**
   * Obtiene los IDs de matches recientes.
   *
   * @param userInput Filtro opcional
   * @return CompletableFuture con la lista de IDs
   */
  public static CompletableFuture<List<String>> getRecentMatchIds(String userInput) {
    return historyService.getRecentMatchIds(userInput);
  }

  /**
   * Realiza un rollback de un match.
   *
   * @param sender Quien ejecuta el rollback
   * @param history Historial del match a revertir
   * @return CompletableFuture que se completa cuando termina el rollback
   */
  public static CompletableFuture<Void> rollbackMatch(
      org.bukkit.command.CommandSender sender, MatchHistory history) {
    return historyService.rollbackMatch(sender, history);
  }

  /**
   * Extrae información de los equipos de un match. Delega a TeamInfoExtractor.
   *
   * @param match El match del cual extraer la información
   * @return Lista de TeamInfo con información de cada equipo
   */
  public static List<TeamInfo> extractTeamInfo(Match match) {
    return TeamInfoExtractor.extractTeamInfo(match);
  }

  /**
   * Crea un MatchStats a partir de PlayerStats de PGM. Delega a MatchStatsCollector.
   *
   * @param playerStats Estadísticas del jugador de PGM
   * @param displayName Nombre del jugador
   * @param totalPoints Puntos totales del jugador
   * @param teamName Nombre del equipo
   * @return MatchStats con todas las estadísticas
   */
  public static MatchStats createMatchStats(
      PlayerStats playerStats, String displayName, int totalPoints, String teamName) {
    return MatchStatsCollector.createMatchStats(playerStats, displayName, totalPoints, teamName);
  }

  /**
   * Crea un mapa de jugador -> TeamInfo. Delega a TeamInfoExtractor.
   *
   * @param match El match del cual extraer la información
   * @return Mapa con username como key y TeamInfo como valor
   */
  public static Map<String, TeamInfo> createPlayerTeamMap(Match match) {
    return TeamInfoExtractor.createPlayerTeamMap(match);
  }
}
