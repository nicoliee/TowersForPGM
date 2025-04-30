package org.nicolie.towersforpgm.draft;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.MatchManager;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.ConfigManager;

import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

public class AvailablePlayers {
    private final List<MatchPlayer> availablePlayers = new ArrayList<>();
    private final List<String> availableOfflinePlayers = new ArrayList<>();
    private final Map<String, PlayerStats> playerStats = new HashMap<>();
    private final MatchManager matchManager;
    private final List<String> topPlayers = new ArrayList<>();

    public AvailablePlayers(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    public void addPlayer(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        MatchPlayer matchPlayer = matchManager.getMatch().getPlayer(player);
        
        // Verificar si el jugador y matchPlayer no son null antes de continuar
        if (player != null && matchPlayer != null && player.isOnline()) {
            // Si el jugador no está en availablePlayers, lo agregamos
            if (availablePlayers.stream().noneMatch(p -> p.getNameLegacy().equalsIgnoreCase(matchPlayer.getNameLegacy()))) {
                availablePlayers.add(matchPlayer);
            }
            // Eliminar de availableOfflinePlayers si está presente
            availableOfflinePlayers.removeIf(name -> name.equalsIgnoreCase(playerName));
            loadStatsForPlayer(playerName);
        } else {
            // Verificar si el jugador está offline y agregarlo a availableOfflinePlayers si no está allí
            if (availableOfflinePlayers.stream().noneMatch(name -> name.equalsIgnoreCase(playerName))) {
                availableOfflinePlayers.add(playerName);
            }
            
            // Solo eliminar de availablePlayers si matchPlayer no es null
            if (matchPlayer != null) {
                availablePlayers.removeIf(p -> p.getNameLegacy().equalsIgnoreCase(matchPlayer.getNameLegacy()));
            }
            
            loadStatsForPlayer(playerName);
        }

        updateTopPlayers();
    }    

    // Método para eliminar jugador
    public void removePlayer(String playerName) {
        availablePlayers.removeIf(p -> p.getNameLegacy().equalsIgnoreCase(playerName));
        availableOfflinePlayers.removeIf(p -> p.equalsIgnoreCase(playerName));
        playerStats.remove(playerName);
        
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
    
    public void clear() {
        if(availablePlayers == null || availableOfflinePlayers == null) return;
        if(availablePlayers.isEmpty() && availableOfflinePlayers.isEmpty()) return;
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
                        playerStats.put(playerName, stats);
                    } else {
                        // Si no hay estadísticas, poner valores predeterminados
                        playerStats.put(playerName, new PlayerStats(0, 0, 0, 0, 0, 0));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                // En caso de error, agregamos estadísticas predeterminadas
                playerStats.put(playerName, new PlayerStats(0, 0, 0, 0, 0, 0));
            }
        });
    }

    public PlayerStats getStatsForPlayer(String playerName) {
        return playerStats.getOrDefault(playerName, new PlayerStats(0, 0, 0, 0, 0, 0));
    }

    private void updateTopPlayers() {
        // Crear una lista temporal con todos los jugadores y sus estadísticas
        List<Map.Entry<String, Double>> playersWithKD = new ArrayList<>();

        for (String playerName : getAllAvailablePlayers()) {
            PlayerStats stats = getStatsForPlayer(playerName);
            int kills = stats.getKills();
            int deaths = stats.getDeaths();
            int assists = stats.getAssists();
            int points = stats.getPoints();
            int wins = stats.getWins();
            int games = stats.getGames();
            double kd = stats.getDeaths() == 0 ? stats.getKills() : (double) kills / deaths;
            playersWithKD.add(new AbstractMap.SimpleEntry<>(playerName, kd));
        }

        // Ordenar por KD descendente
        playersWithKD.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Actualizar la lista de mejores jugadores 
        topPlayers.clear();
        for (Map.Entry<String, Double> entry : playersWithKD) {
            topPlayers.add(entry.getKey());
        }
    }

    public List<String> getTopPlayers() {
        return new ArrayList<>(topPlayers);
    }
}