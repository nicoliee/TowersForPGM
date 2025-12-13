package org.nicolie.towersforpgm.draft.core;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final org.nicolie.towersforpgm.configs.ConfigManager configManager;
  private final List<MatchPlayer> availablePlayers = new ArrayList<>();
  private final List<String> availableOfflinePlayers = new ArrayList<>();
  private final Map<String, Stats> playerStats = new HashMap<>();
  private final List<String> topPlayers = new ArrayList<>();

  public AvailablePlayers(org.nicolie.towersforpgm.configs.ConfigManager configManager) {
    this.configManager = configManager;
  }

  public void addPlayer(String playerName) {
    Player player = Bukkit.getPlayerExact(playerName);
    Match match = PGM.get().getMatchManager().getMatch(player);
    MatchPlayer matchPlayer = match != null ? match.getPlayer(player) : null;

    // Verificar si el jugador y matchPlayer no son null antes de continuar
    if (match != null && player != null && matchPlayer != null && player.isOnline()) {
      // Si el jugador no está en availablePlayers, lo agregamos
      if (availablePlayers.stream()
          .noneMatch(p -> p.getNameLegacy().equalsIgnoreCase(matchPlayer.getNameLegacy()))) {
        availablePlayers.add(matchPlayer);
      }
      // Eliminar de availableOfflinePlayers si está presente
      availableOfflinePlayers.removeIf(name -> name.equalsIgnoreCase(playerName));
      if (TowersForPGM.getInstance().getIsDatabaseActivated()) {
        loadStatsForPlayer(playerName);
      }
    } else {
      // Verificar si el jugador está offline y agregarlo a availableOfflinePlayers si
      // no está allí
      if (availableOfflinePlayers.stream().noneMatch(name -> name.equalsIgnoreCase(playerName))) {
        availableOfflinePlayers.add(playerName);
      }

      // Solo eliminar de availablePlayers si matchPlayer no es null
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

    // Remover de la lista de mejores jugadores
    topPlayers.removeIf(name -> name.equalsIgnoreCase(playerName));
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
    if (availablePlayers.isEmpty() && availableOfflinePlayers.isEmpty()) return;
    availablePlayers.clear();
    availableOfflinePlayers.clear();
    playerStats.clear();
    topPlayers.clear();
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
                new Stats(playerName, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, elo, lastElo, maxElo);
            playerStats.put(playerName, defaultStats);
          }
        })
        .exceptionally(throwable -> {
          // En caso de error, agregamos estadísticas predeterminadas
          int elo = isRanked ? 0 : -9999;
          int lastElo = isRanked ? 0 : -9999;
          int maxElo = isRanked ? 0 : -9999;
          Stats defaultStats =
              new Stats(playerName, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, elo, lastElo, maxElo);
          playerStats.put(playerName, defaultStats);
          return null;
        });
  }

  public Stats getStatsForPlayer(String playerName) {
    return playerStats.getOrDefault(
        playerName, new Stats(playerName, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -9999, -9999, -9999));
  }

  private void updateTopPlayers() {
    List<Map.Entry<String, Double>> playersWithRating = new ArrayList<>();

    for (String playerName : getAllAvailablePlayers()) {
      Stats stats = getStatsForPlayer(playerName);
      double rating = MMR.compute(stats);
      playersWithRating.add(new AbstractMap.SimpleEntry<>(playerName, rating));
    }

    // Ordenar la lista por puntuación (de mayor a menor)
    playersWithRating.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

    // Actualizar la lista de mejores jugadores
    topPlayers.clear();
    for (Map.Entry<String, Double> entry : playersWithRating) {
      topPlayers.add(entry.getKey());
    }
  }

  public List<String> getTopPlayers() {
    return new ArrayList<>(topPlayers);
  }
}
