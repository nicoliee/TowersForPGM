package org.nicolie.towersforpgm.draft;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.MatchManager;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.ConfigManager;

import tc.oc.pgm.api.player.MatchPlayer;

public class AvailablePlayers {
    private final List<MatchPlayer> availablePlayers = new ArrayList<>();
    private final List<String> availableOfflinePlayers = new ArrayList<>();
    private final Map<String, PlayerStats> playerStatsCache = new HashMap<>();
    private final MatchManager matchManager;

    public AvailablePlayers(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    public void addPlayer(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        MatchPlayer matchPlayer = matchManager.getMatch().getPlayer(player);
        if (player != null && player.isOnline()) {
            if (availablePlayers.stream().noneMatch(p -> p.getNameLegacy().equalsIgnoreCase(matchPlayer.getNameLegacy()))) {
                availablePlayers.add(matchPlayer);
            }
            availableOfflinePlayers.removeIf(name -> name.equalsIgnoreCase(playerName));
            loadStatsForPlayer(playerName);
        } else {
            if (availableOfflinePlayers.stream().noneMatch(name -> name.equalsIgnoreCase(playerName))) {
                availableOfflinePlayers.add(playerName);
            }
            availablePlayers.removeIf(p -> p.getNameLegacy().equalsIgnoreCase(matchPlayer.getNameLegacy()));
            loadStatsForPlayer(playerName);
        }
    }

    // Método para eliminar jugador
    public void removePlayer(String playerName) {
        availablePlayers.removeIf(p -> p.getNameLegacy().equalsIgnoreCase(playerName));
        availableOfflinePlayers.removeIf(p -> p.equalsIgnoreCase(playerName));
        playerStatsCache.remove(playerName);
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
    
    public void clear() {
        if(availablePlayers == null || availableOfflinePlayers == null) return;
        if(availablePlayers.isEmpty() && availableOfflinePlayers.isEmpty()) return;
        availablePlayers.clear();
        availableOfflinePlayers.clear();
        playerStatsCache.clear();
    }

    public void handleDisconnect(Player player) {
        MatchPlayer matchPlayer = matchManager.getMatch().getPlayer(player);
        // Remover de online si está
        if (availablePlayers.remove(matchPlayer)) {
            // Agregar a offline por nombre
            String name = player.getName();
            availableOfflinePlayers.add(name);
        }
    }
    
    public void handleReconnect(Player player) {
        MatchPlayer matchPlayer = matchManager.getMatch().getPlayer(player);
        String name = player.getName();
        // Si estaba en la lista de offline, lo quitamos
        if (availableOfflinePlayers.removeIf(p -> p.equalsIgnoreCase(name))) {
            // Lo agregamos como Player online
            availablePlayers.add(matchPlayer);
        }
    }

    public void loadStatsForPlayer(String playerName) {
        // Verificar si ya tenemos las estadísticas del jugador en cache
        if (playerStatsCache.containsKey(playerName)) {
            return; // Ya están cargadas
        }

        Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
            String sql = "SELECT kills, deaths, assists, points, wins, games FROM " + ConfigManager.getTableForMap(matchManager.getMatch().getMap().getName()) + " WHERE username = ?";
            
            try (Connection conn = TowersForPGM.getInstance().getDatabaseManager().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, playerName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // Guardar las estadísticas del jugador
                        PlayerStats stats = new PlayerStats(
                            rs.getInt("kills"),
                            rs.getInt("deaths"),
                            rs.getInt("assists"),
                            rs.getInt("points"),
                            rs.getInt("wins"),
                            rs.getInt("games")
                        );
                        playerStatsCache.put(playerName, stats);
                    } else {
                        // Si no hay estadísticas, poner valores predeterminados
                        playerStatsCache.put(playerName, new PlayerStats(0, 0, 0, 0, 0, 0));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                // En caso de error, agregamos estadísticas predeterminadas
                playerStatsCache.put(playerName, new PlayerStats(0, 0, 0, 0, 0, 0));
            }
        });
    }

    public PlayerStats getStatsForPlayer(String playerName) {
        return playerStatsCache.getOrDefault(playerName, new PlayerStats(0, 0, 0, 0, 0, 0));
    }
}