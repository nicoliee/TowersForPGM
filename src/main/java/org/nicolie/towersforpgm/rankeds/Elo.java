package org.nicolie.towersforpgm.rankeds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.utils.ConfigManager;
import tc.oc.pgm.api.player.MatchPlayer;

/**
 * Clase encargada de gestionar el sistema de ranking Elo para partidas competitivas. El sistema Elo
 * es un método de calificación utilizado para calcular los niveles relativos de habilidad de los
 * jugadores en juegos de suma cero.
 *
 * <p>Esta implementación incluye factores adicionales como: - Habilidad individual del jugador
 * comparada con su equipo - Diferencia de fuerza entre equipos - Límites mínimos y máximos de
 * cambio de Elo
 */
public class Elo {
  private static final int K_FACTOR = 32;

  /**
   * Método principal para calcular y aplicar cambios de Elo después de una partida. Procesa tanto a
   * los ganadores como a los perdedores, calculando cambios individualizados basados en múltiples
   * factores de rendimiento.
   *
   * @param winners Lista de jugadores del equipo ganador
   * @param losers Lista de jugadores del equipo perdedor
   * @return CompletableFuture que contiene la lista de cambios de Elo para todos los jugadores
   */
  public static CompletableFuture<List<PlayerEloChange>> addWin(
      List<MatchPlayer> winners, List<MatchPlayer> losers) {
    CompletableFuture<List<PlayerEloChange>> future = new CompletableFuture<>();
    List<PlayerEloChange> changes = new ArrayList<>();

    // Combinar todos los jugadores de ambos equipos para obtener sus datos de Elo
    List<MatchPlayer> allPlayers = new ArrayList<>();
    allPlayers.addAll(winners);
    allPlayers.addAll(losers);
    List<String> usernames =
        allPlayers.stream().map(MatchPlayer::getNameLegacy).collect(Collectors.toList());

    // Obtener la tabla de ranking configurada
    String table = ConfigManager.getRankedDefaultTable();
    if (table == null || table.isEmpty()) {
      org.bukkit.Bukkit.getLogger().warning("[Elo.addWin] Table is null or empty");
      future.completeExceptionally(new RuntimeException("Table is null or empty"));
      return future;
    }

    // Obtener los datos actuales de Elo de todos los jugadores de forma asíncrona
    StatsManager.getEloForUsernames(table, usernames, eloList -> {
      // Crear mapas para acceso rápido a los datos de Elo
      Map<String, PlayerEloChange> eloMap =
          eloList.stream().collect(Collectors.toMap(PlayerEloChange::getUsername, e -> e));
      Map<String, Integer> currentEloMap = eloMap.entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getCurrentElo()));

      // Verificar si es empate (lista de perdedores vacía)
      if (losers.isEmpty()) {
        // En caso de empate, todos los jugadores reciben 0 puntos de Elo
        winners.forEach(player -> {
          String username = player.getNameLegacy();
          PlayerEloChange playerElo =
              eloMap.getOrDefault(username, new PlayerEloChange(username, 0, 0, 0, 0));
          int currentElo = playerElo.getCurrentElo();
          int maxElo = playerElo.getMaxElo();
          changes.add(new PlayerEloChange(username, currentElo, currentElo, 0, maxElo));
        });
        future.complete(changes);
        return;
      }

      // Calcular los promedios de Elo de cada equipo para comparaciones de línea base
      double winnersAvgElo = calculateTeamAverageElo(winners, currentEloMap);
      double losersAvgElo = calculateTeamAverageElo(losers, currentEloMap);

      // Procesar cada jugador ganador
      winners.forEach(winner -> {
        String username = winner.getNameLegacy();
        PlayerEloChange playerElo =
            eloMap.getOrDefault(username, new PlayerEloChange(username, 0, 0, 0, 0));
        int currentElo = playerElo.getCurrentElo();

        // Calcular el cambio individual de Elo basado en el rendimiento del jugador vs expectativa
        int individualEloChange =
            calculateIndividualEloChange(currentElo, winnersAvgElo, losersAvgElo, true);

        int newElo = currentElo + individualEloChange;
        int maxElo =
            Math.max(newElo, playerElo.getMaxElo()); // Actualizar Elo máximo si es necesario
        changes.add(new PlayerEloChange(username, currentElo, newElo, individualEloChange, maxElo));
      });

      // Procesar cada jugador perdedor
      losers.forEach(loser -> {
        String username = loser.getNameLegacy();
        PlayerEloChange playerElo =
            eloMap.getOrDefault(username, new PlayerEloChange(username, 0, 0, 0, 0));
        int currentElo = playerElo.getCurrentElo();

        // Calcular el cambio individual de Elo basado en el rendimiento del jugador vs expectativa
        int individualEloChange =
            calculateIndividualEloChange(currentElo, losersAvgElo, winnersAvgElo, false);

        // Aplicar límite mínimo de Elo (-100) para evitar valores excesivamente negativos
        int newElo = Math.max(-100, currentElo + individualEloChange);
        int maxElo = playerElo.getMaxElo(); // El Elo máximo no cambia al perder
        changes.add(new PlayerEloChange(username, currentElo, newElo, individualEloChange, maxElo));
      });

      future.complete(changes);
    });

    return future;
  }

  /**
   * Obtiene y ordena los jugadores por su Elo actual de mayor a menor. También calcula el Elo
   * promedio del grupo de jugadores.
   *
   * @param players Lista de jugadores a ordenar
   * @param callback Función callback que recibe la lista ordenada y el Elo promedio
   */
  public static void eloOrder(
      List<MatchPlayer> players,
      java.util.function.BiConsumer<List<PlayerEloChange>, Integer> callback) {
    // Extraer nombres de usuario de los jugadores
    List<String> usernames =
        players.stream().map(MatchPlayer::getNameLegacy).collect(Collectors.toList());
    String table = ConfigManager.getRankedDefaultTable();

    // Validar que la tabla de configuración existe
    if (table == null || table.isEmpty()) {
      callback.accept(null, 0);
      return;
    }

    // Obtener datos de Elo y procesarlos
    StatsManager.getEloForUsernames(table, usernames, eloList -> {
      // Ordenar jugadores por Elo descendente (mayor a menor)
      List<PlayerEloChange> sortedList = eloList.stream()
          .sorted((e1, e2) -> Integer.compare(e2.getCurrentElo(), e1.getCurrentElo()))
          .collect(Collectors.toList());

      // Calcular Elo promedio del grupo
      int avgElo = 0;
      if (!sortedList.isEmpty()) {
        int total = sortedList.stream().mapToInt(PlayerEloChange::getCurrentElo).sum();
        avgElo = total / sortedList.size();
      }
      callback.accept(sortedList, avgElo);
    });
  }

  /**
   * Calcula el Elo promedio de un equipo basado en el Elo actual de sus jugadores.
   *
   * @param team Lista de jugadores del equipo
   * @param eloMap Mapa que contiene el Elo actual de cada jugador
   * @return Promedio de Elo del equipo, 0.0 si el equipo está vacío
   */
  private static double calculateTeamAverageElo(
      List<MatchPlayer> team, Map<String, Integer> eloMap) {
    if (team.isEmpty()) return 0.0;

    double totalElo = 0.0;
    for (MatchPlayer player : team) {
      // Obtener Elo del jugador, usar 0 si no existe en el mapa
      totalElo += eloMap.getOrDefault(player.getNameLegacy(), 0);
    }
    return totalElo / team.size();
  }

  /**
   * Calcula el cambio de Elo individual para un jugador basado en múltiples factores. Este
   * algoritmo considera: 1. La puntuación esperada según la fórmula estándar de Elo 2. La habilidad
   * individual del jugador comparada con su equipo 3. La diferencia de fuerza entre los equipos
   *
   * @param playerElo Elo actual del jugador
   * @param teamAvgElo Elo promedio del equipo del jugador
   * @param opponentAvgElo Elo promedio del equipo oponente
   * @param isWinner true si el jugador ganó, false si perdió
   * @return Cambio de Elo calculado (puede ser positivo o negativo)
   */
  private static int calculateIndividualEloChange(
      int playerElo, double teamAvgElo, double opponentAvgElo, boolean isWinner) {
    // Calcular puntuación esperada usando la fórmula estándar de Elo
    double playerExpectedScore = 1.0 / (1.0 + Math.pow(10.0, (opponentAvgElo - playerElo) / 400.0));
    double actualScore = isWinner ? 1.0 : 0.0; // 1 para victoria, 0 para derrota

    // Cambio base de Elo usando el factor K
    int baseEloChange = (int) Math.round(K_FACTOR * (actualScore - playerExpectedScore));

    // FACTOR 1: Habilidad individual del jugador relativa a su equipo
    double skillDifference = playerElo - teamAvgElo;
    double individualScalingFactor;

    if (isWinner) {
      // Para ganadores: jugadores más fuertes que su equipo ganan menos Elo
      individualScalingFactor = 1.0 - (skillDifference / 800.0);
      individualScalingFactor = Math.max(0.7, Math.min(1.3, individualScalingFactor));
    } else {
      // Para perdedores: jugadores más fuertes que su equipo pierden menos Elo
      individualScalingFactor = 1.0 + (skillDifference / 800.0);
      individualScalingFactor = Math.max(0.7, Math.min(1.3, individualScalingFactor));
    }

    // FACTOR 2: Diferencia de fuerza entre equipos
    double teamStrengthDifference = teamAvgElo - opponentAvgElo;
    double teamBalanceScalingFactor;

    if (isWinner) {
      // Para ganadores: ganar contra equipos más débiles da menos Elo
      teamBalanceScalingFactor = 1.0 - (teamStrengthDifference / 600.0);
      teamBalanceScalingFactor = Math.max(0.5, Math.min(1.5, teamBalanceScalingFactor));
    } else {
      // Para perdedores: perder contra equipos más fuertes quita menos Elo
      teamBalanceScalingFactor = 1.0 + (teamStrengthDifference / 600.0);
      teamBalanceScalingFactor = Math.max(0.5, Math.min(1.5, teamBalanceScalingFactor));
    }

    // Combinar ambos factores de escalado
    double combinedScalingFactor = individualScalingFactor * teamBalanceScalingFactor;
    int scaledEloChange = (int) Math.round(baseEloChange * combinedScalingFactor);

    // Aplicar límites mínimos y máximos para evitar cambios extremos
    int minChange = isWinner ? 3 : -45; // Mínimo: +3 para ganar, -45 para perder
    int maxChange = isWinner ? 45 : -3; // Máximo: +45 para ganar, -3 para perder

    return Math.max(minChange, Math.min(maxChange, scaledEloChange));
  }
}
