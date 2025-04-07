package org.nicolie.towersforpgm.draft;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.MatchManager;

import tc.oc.pgm.api.player.MatchPlayer;

import java.util.HashSet;
import java.util.Set;

public class Teams {
    private final Set<MatchPlayer> team1 = new HashSet<>();
    private final Set<MatchPlayer> team2 = new HashSet<>();
    private final Set<String> team1Offline = new HashSet<>();
    private final Set<String> team2Offline = new HashSet<>();
    private final MatchManager matchManager;

    public Teams(MatchManager matchManager) {
        this.matchManager = matchManager;
    }
    
    public void addPlayerToTeam(String playerName, int teamNumber) {
        Player player = Bukkit.getPlayer(playerName);
        MatchPlayer matchPlayer = matchManager.getMatch().getPlayer(player);
        if (player != null && playerName == matchPlayer.getNameLegacy()) {
            getTeam(teamNumber).add(matchPlayer);
        } else {
            getOfflineTeam(teamNumber).add(playerName);
        }
    }

    public void removePlayerFromTeam(String playerName, int teamNumber) {
        Player player = Bukkit.getPlayer(playerName);
        MatchPlayer matchPlayer = matchManager.getMatch().getPlayer(player);
        if (player != null) {
            getTeam(teamNumber).remove(matchPlayer);
        } else {
            getOfflineTeam(teamNumber).remove(playerName);
        }
    }

    public boolean isPlayerInTeam(String playerName, int teamNumber) {
        Player player = Bukkit.getPlayer(playerName);
        MatchPlayer matchPlayer = matchManager.getMatch().getPlayer(player);
        return getTeam(teamNumber).contains(matchPlayer) || getOfflineTeam(teamNumber).contains(playerName);
    }

    public boolean isPlayerInAnyTeam(String playerName) {
        return isPlayerInTeam(playerName, 1) || isPlayerInTeam(playerName, 2);
    }

    public void clearTeams() {
        team1.clear();
        team2.clear();
        team1Offline.clear();
        team2Offline.clear();
    }

    private Set<MatchPlayer> getTeam(int teamNumber) {
        return (teamNumber == 1) ? team1 : team2;
    }

    private Set<String> getOfflineTeam(int teamNumber) {
        return (teamNumber == 1) ? team1Offline : team2Offline;
    }

    public Set<String> getAllTeam(int teamNumber) {
        Set<String> allPlayers = new HashSet<>();
        
        // Añadir jugadores desconectados al set
        allPlayers.addAll(getOfflineTeam(teamNumber)); // Esto debería estar correcto si getOfflineTeam devuelve bien los nombres.
        
        // Añadir jugadores conectados al set
        for (MatchPlayer player : getTeam(teamNumber)) {
            // Aseguramos que el nombre es el correcto y no el legacy
            String playerName = player.getPlayer().getNameLegacy(); // Nombre actual
            allPlayers.add(playerName);
        }
        
        return allPlayers;
    }
    
}