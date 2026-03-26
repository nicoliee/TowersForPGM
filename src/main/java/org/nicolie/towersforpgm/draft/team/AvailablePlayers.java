package org.nicolie.towersforpgm.draft.team;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.models.MMR;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class AvailablePlayers {
  private final java.util.Set<String> suggestedAdvisors = new java.util.HashSet<>();

  public boolean hasAlreadySuggested(String advisorName) {
    return suggestedAdvisors.contains(advisorName);
  }

  public void recordSuggestion(String advisorName) {
    suggestedAdvisors.add(advisorName);
  }

  public void clearSuggestions() {
    suggestedAdvisors.clear();
  }

  private final org.nicolie.towersforpgm.configs.ConfigManager configManager;
  private final List<MatchPlayer> availablePlayers = new ArrayList<>();
  private final List<String> availableOfflinePlayers = new ArrayList<>();
  private final Map<String, Stats> playerStats = new HashMap<>();
  private final List<String> topPlayers = new ArrayList<>();
  private final List<Map.Entry<String, Integer>> pickHistory = new ArrayList<>();

  public AvailablePlayers(org.nicolie.towersforpgm.configs.ConfigManager configManager) {
    this.configManager = configManager;
  }

  public void addPlayer(String playerName) {
    Player player = Bukkit.getPlayerExact(playerName);
    Match match = PGM.get().getMatchManager().getMatch(player);
    MatchPlayer matchPlayer = match != null ? match.getPlayer(player) : null;

    if (match != null && player != null && matchPlayer != null && player.isOnline()) {
      if (availablePlayers.stream()
          .noneMatch(p -> p.getNameLegacy().equalsIgnoreCase(matchPlayer.getNameLegacy()))) {
        availablePlayers.add(matchPlayer);
      }
      availableOfflinePlayers.removeIf(name -> name.equalsIgnoreCase(playerName));
      if (TowersForPGM.getInstance().getIsDatabaseActivated()) {
        loadStatsForPlayer(playerName);
      }
    } else {
      if (availableOfflinePlayers.stream().noneMatch(name -> name.equalsIgnoreCase(playerName))) {
        availableOfflinePlayers.add(playerName);
      }

      if (matchPlayer != null) {
        availablePlayers.removeIf(
            p -> p.getNameLegacy().equalsIgnoreCase(matchPlayer.getNameLegacy()));
      }

      if (TowersForPGM.getInstance().getIsDatabaseActivated()) {
        loadStatsForPlayer(playerName);
      }
    }
    if (TowersForPGM.getInstance().getIsDatabaseActivated()) {
      updateTopPlayers();
    }
  }

  // Método para eliminar jugador
  public void removePlayer(String playerName) {
    availablePlayers.removeIf(p -> p.getNameLegacy().equalsIgnoreCase(playerName));
    availableOfflinePlayers.removeIf(p -> p.equalsIgnoreCase(playerName));
    if (TowersForPGM.getInstance().getIsDatabaseActivated()) {
      playerStats.remove(playerName);
    }

    topPlayers.removeIf(name -> name.equalsIgnoreCase(playerName));
  }

  public String randomPick() {
    int size = getAllAvailablePlayers().size();
    // Si la base de datos está desactivada, devolver un usuario aleatorio de los disponibles
    if (!TowersForPGM.getInstance().getIsDatabaseActivated()) {
      List<String> allPlayers = new ArrayList<>(getAllAvailablePlayers());
      if (allPlayers.isEmpty()) {
        return null;
      }
      java.util.Collections.shuffle(allPlayers);
      return allPlayers.get(0);
    }
    List<String> topPlayers = getTopPlayers();
    if (topPlayers.isEmpty() || size == 0) {
      return null;
    }
    if (size > 6) {
      topPlayers = new ArrayList<>(topPlayers.subList(0, 3)); // Limitar a los 3 mejores jugadores
    } else if (size > 3) {
      topPlayers = new ArrayList<>(topPlayers.subList(0, 2)); // Limitar a los 2 mejores jugadores
    } else if (size <= 3) {
      topPlayers = new ArrayList<>(topPlayers.subList(0, 1)); // Limitar a los 1 mejores jugadores
    }
    java.util.Collections.shuffle(topPlayers); // Ordenar aleatoriamente las sugerencias
    return topPlayers.get(0); // Devolver el primer jugador de la lista aleatoria
  }

  public void recordPick(String playerName, int teamNumber) {
    pickHistory.add(new AbstractMap.SimpleEntry<>(playerName, teamNumber));
  }

  public List<Map.Entry<String, Integer>> getPickHistory() {
    return new ArrayList<>(pickHistory);
  }

  public String getExactUser(String playerName) {
    // Buscar en jugadores online
    for (MatchPlayer matchPlayer : availablePlayers) {
      if (matchPlayer.getNameLegacy().equalsIgnoreCase(playerName)) {
        return matchPlayer.getNameLegacy();
      }
    }
    // Buscar en jugadores offline
    for (String offlinePlayer : availableOfflinePlayers) {
      if (offlinePlayer.equalsIgnoreCase(playerName)) {
        return offlinePlayer;
      }
    }
    return null;
  }

  public List<MatchPlayer> getAvailablePlayers() {
    return new ArrayList<>(availablePlayers);
  }

  public List<String> getAvailableOfflinePlayers() {
    return new ArrayList<>(availableOfflinePlayers);
  }

  public List<String> getAllAvailablePlayers() {
    List<String> allAvailablePlayers = new ArrayList<>();
    availablePlayers.forEach(player -> allAvailablePlayers.add(player.getNameLegacy()));
    allAvailablePlayers.addAll(availableOfflinePlayers);
    return allAvailablePlayers;
  }

  public List<UUID> getAllAvailablePlayersUUIDs() {
    List<UUID> uuids = new ArrayList<>();
    availablePlayers.forEach(player -> uuids.add(player.getId()));
    availableOfflinePlayers.forEach(name -> {
      Player player = Bukkit.getPlayerExact(name);
      if (player != null) {
        uuids.add(player.getUniqueId());
      }
    });
    return uuids;
  }

  public boolean isEmpty() {
    return availablePlayers.isEmpty() && availableOfflinePlayers.isEmpty();
  }

  public boolean isPlayerAvailable(String playerName) {
    return availablePlayers.stream()
            .anyMatch(player -> player.getNameLegacy().equalsIgnoreCase(playerName))
        || availableOfflinePlayers.stream().anyMatch(name -> name.equalsIgnoreCase(playerName));
  }

  public void clear() {
    if (availablePlayers == null || availableOfflinePlayers == null) return;
    availablePlayers.clear();
    availableOfflinePlayers.clear();
    playerStats.clear();
    topPlayers.clear();
    pickHistory.clear();
  }

  public void handleDisconnect(Player player) {
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getMatch(player).getPlayer(player);
    // Remover de online si está
    if (availablePlayers.remove(matchPlayer)) {
      // Agregar a offline por nombre
      String name = player.getName();
      availableOfflinePlayers.add(name);
    }
  }

  public void handleReconnect(Player player) {
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getMatch(player).getPlayer(player);
    String name = player.getName();
    // Si estaba en la lista de offline, lo quitamos
    if (availableOfflinePlayers.removeIf(p -> p.equalsIgnoreCase(name))) {
      // Lo agregamos como Player online
      availablePlayers.add(matchPlayer);
    }
  }

  public void loadStatsForPlayer(String playerName) {
    // Verificar si ya tenemos las estadísticas del jugador en cache
    if (playerStats.containsKey(playerName)) {
      return; // Ya están cargadas
    }

    String table =
        configManager.databaseTables().getTable(MatchManager.getMatch().getMap().getName());
    boolean isRanked = Queue.isRanked();

    org.nicolie.towersforpgm.database.StatsManager.getStats(table, playerName)
        .thenAccept(stats -> {
          if (stats != null) {
            playerStats.put(playerName, stats);
          } else {
            int elo = isRanked ? 0 : -9999;
            int lastElo = isRanked ? 0 : -9999;
            int maxElo = isRanked ? 0 : -9999;
            Stats defaultStats =
                new Stats(playerName, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, elo, lastElo, maxElo);
            playerStats.put(playerName, defaultStats);
          }
        })
        .exceptionally(throwable -> {
          // En caso de error, agregamos estadísticas predeterminadas
          int elo = isRanked ? 0 : -9999;
          int lastElo = isRanked ? 0 : -9999;
          int maxElo = isRanked ? 0 : -9999;
          Stats defaultStats =
              new Stats(playerName, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, elo, lastElo, maxElo);
          playerStats.put(playerName, defaultStats);
          return null;
        });
  }

  public Stats getStatsForPlayer(String playerName) {
    return playerStats.getOrDefault(
        playerName, new Stats(playerName, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -9999, -9999, -9999));
  }

  private void updateTopPlayers() {
    List<Map.Entry<String, Double>> playersWithRating = new ArrayList<>();

    for (String playerName : getAllAvailablePlayers()) {
      Stats stats = getStatsForPlayer(playerName);
      double rating = MMR.compute(stats);
      playersWithRating.add(new AbstractMap.SimpleEntry<>(playerName, rating));
    }

    playersWithRating.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

    topPlayers.clear();
    for (Map.Entry<String, Double> entry : playersWithRating) {
      topPlayers.add(entry.getKey());
    }
  }

  public List<String> getTopPlayers() {
    return new ArrayList<>(topPlayers);
  }
}
