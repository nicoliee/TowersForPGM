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
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class AvailablePlayers {
    private final List<MatchPlayer> availablePlayers = new ArrayList<>();
    private final List<String> availableOfflinePlayers = new ArrayList<>();
    private final Map<String, PlayerStats> playerStats = new HashMap<>();
    private final List<String> topPlayers = new ArrayList<>();


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
                availablePlayers.removeIf(p -> p.getNameLegacy().equalsIgnoreCase(matchPlayer.getNameLegacy()));
            }

            if (TowersForPGM.getInstance().getIsDatabaseActivated()) {
                loadStatsForPlayer(playerName);
            }
        }
        if (TowersForPGM.getInstance().getIsDatabaseActivated()){
            updateTopPlayers();
        }
    }

    // Método para eliminar jugador
    public void removePlayer(String playerName) {
        availablePlayers.removeIf(p -> p.getNameLegacy().equalsIgnoreCase(playerName));
        availableOfflinePlayers.removeIf(p -> p.equalsIgnoreCase(playerName));
        if (TowersForPGM.getInstance().getIsDatabaseActivated()){
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

    public void clear() {
        if (availablePlayers == null || availableOfflinePlayers == null)
            return;
        if (availablePlayers.isEmpty() && availableOfflinePlayers.isEmpty())
            return;
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

        String table;
            if (ConfigManager.getTempTable() != null) {
                table = ConfigManager.getTempTable();
            } else {
                table = ConfigManager.getTableForMap(MatchManager.getMatch().getMap().getName());
            }
            boolean isRanked = ConfigManager.getRankedTables().contains(table); 

        Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
            String sql;
            if (isRanked) {
                sql = "SELECT elo, kills, deaths, assists, damageDone, damageTaken, points, wins, games FROM "
                        + table + " WHERE username = ?";
            } else {
                sql = "SELECT kills, deaths, assists, damageDone, damageTaken, points, wins, games FROM "
                        + table + " WHERE username = ?";
            }

            try (Connection conn = TowersForPGM.getInstance().getDatabaseManager().getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerName);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // Guardar las estadísticas del jugador
                        int elo = isRanked ? rs.getInt("elo") : -9999;
                        PlayerStats stats = new PlayerStats(
                                elo,
                                rs.getInt("kills"),
                                rs.getInt("deaths"),
                                rs.getInt("assists"),
                                rs.getDouble("damageDone"),
                                rs.getDouble("damageTaken"),
                                rs.getInt("points"),
                                rs.getInt("wins"),
                                rs.getInt("games"));
                        playerStats.put(playerName, stats);
                    } else {
                        // Si no hay estadísticas, poner valores predeterminados
                        int elo = isRanked ? 0 : -9999;
                        playerStats.put(playerName, new PlayerStats(elo, 0, 0, 0, 0, 0, 0, 0, 0));
                    }
                }
            } catch (SQLException e) {
                // En caso de error, agregamos estadísticas predeterminadas
                int elo = isRanked ? 0 : -9999;
                playerStats.put(playerName, new PlayerStats(elo,0, 0, 0, 0, 0, 0, 0, 0));
            }
        });
    }

    public PlayerStats getStatsForPlayer(String playerName) {
        return playerStats.getOrDefault(playerName, new PlayerStats(-9999,0, 0, 0, 0, 0, 0, 0, 0));
    }

    private void updateTopPlayers() {
        List<Map.Entry<String, Double>> playersWithRating = new ArrayList<>();

        for (String playerName : getAllAvailablePlayers()) {
            PlayerStats stats = getStatsForPlayer(playerName);

            int kills = stats.getKills();
            int deaths = stats.getDeaths();
            int assists = stats.getAssists();
            double damageDone = stats.getDamageDone();
            double damageTaken = stats.getDamageTaken();
            int points = stats.getPoints();
            int wins = stats.getWins();
            int games = stats.getGames();

            // Promedios de estadísticas generales
            double maxKills = 350.0;
            double maxDeaths = 250.0;
            double maxAssists = 100.0;
            double maxPointsPerGame = 5.0;

            // Calcular métricas por partida
            double killsPerGame = games == 0 ? 0.0 : kills / (double) games;
            double deathsPerGame = games == 0 ? 0.0 : deaths / (double) games;
            double assistsPerGame = games == 0 ? 0.0 : assists / (double) games;
            double damageDonePerGame = games == 0 ? 0.0 : damageDone / (double) games;
            double damageTakenPerGame = games == 0 ? 0.0 : damageTaken / (double) games;
            double pointsPerGame = games == 0 ? 0.0 : points / (double) games;
            double winRate = games == 0 ? 0.0 : wins / (double) games;

            // Inicializar el rating
            double rating = 0;

            // Ajuste para kills por juego
            rating += (killsPerGame / maxKills) * 3;

            // Penalización para muertes por juego
            rating -= (deathsPerGame / maxDeaths) * 2;

            // Ajuste para asistencias por juego
            rating += (assistsPerGame / maxAssists) * 1.5;

            // Ajuste por daño hecho vs recibido
            if (damageTakenPerGame > 0) {
                double damageRatio = (damageDonePerGame - damageTakenPerGame) / damageTakenPerGame;
                rating += damageRatio * 1.5;
            }

            // Ajuste para puntos por juego
            rating += (pointsPerGame / maxPointsPerGame) * 4;

            // Ajuste para win rate (escalado si menos de 10 partidas)
            if (games >= 10) {
                rating += winRate * 5;
            } else {
                rating += winRate * 5 * (games / 10.0);
            }

            // Bonus por consistencia si ha jugado más de 15 partidas
            if (games >= 12) {
                double kdaStability = 1.0
                        - (Math.abs(killsPerGame - deathsPerGame) + Math.abs(assistsPerGame - killsPerGame)) / maxKills;
                rating += kdaStability * 1.5;
            }

            // Añadir a la lista
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