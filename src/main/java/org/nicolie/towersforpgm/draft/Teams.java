package org.nicolie.towersforpgm.draft;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.MatchManager;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.join.JoinResult;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    public void clear() {
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

    public void assignTeam(Player player, int teamNumber) {
        Match match = matchManager.getMatch();
        if (match == null) {
            return;
        }

        TeamMatchModule teamModule = match.needModule(TeamMatchModule.class);
        if (teamModule == null) {
            return;
        }

        List<Team> teams = new ArrayList<>(teamModule.getTeams());
        Team targetTeam = null;
        // Buscar el equipo por nombre en lugar de por índice
        for (Team team : teams) {
            if (teamNumber == 1 && team.getDefaultName().equalsIgnoreCase("red")) { // hardcoded para el team rojo
                targetTeam = team; // Asignar al equipo "red"
                break;
            } else if (teamNumber == 2 && team.getDefaultName().equalsIgnoreCase("blue")) { // hardcoded para el team azul
                targetTeam = team; // Asignar al equipo "azul"
                break;
            }
        }

        if (targetTeam == null) {
            return; // Si no encontramos el equipo, no hacemos nada
        }
        
        MatchPlayer matchPlayer = match.getPlayer(player);
        if (matchPlayer != null) {
            // Aquí forzamos la unión al equipo sin verificar los permisos
            JoinRequest request = JoinRequest.fromPlayer(matchPlayer, targetTeam, JoinRequest.Flag.FORCE);
            JoinResult result = teamModule.queryJoin(matchPlayer, request);

            // Si no queremos verificar el resultado de la unión, directamente unimos al jugador
            if (result.isSuccess() || !result.isSuccess()) {
                teamModule.join(matchPlayer, request, result);
            }
        }
    }

    public void setTeamsSize(int teamSize) {
        Match match = matchManager.getMatch();
        if (match == null) {
            return;
        }
        TeamMatchModule teamModule = match.needModule(TeamMatchModule.class);
        if (teamModule == null) {
            return;
        }

        List<Team> teams = new ArrayList<>(teamModule.getTeams());
        for (Team team : teams) {
            team.setMaxSize(teamSize, 25);
        }
    }

    public void removeFromAnyTeam(String playerName) {
        if (isPlayerInAnyTeam(playerName)) {
            if (isPlayerInTeam(playerName, 1)) {
                removePlayerFromTeam(playerName, 1);
            } else if (isPlayerInTeam(playerName, 2)) {
                removePlayerFromTeam(playerName, 2);
            }
        }
    }
    
    public void removeFromTeams(){
        Match match = matchManager.getMatch();
        if (match == null) {
            return;
        }
        match.getPlayers().forEach(player -> {
            match.setParty(player, match.getDefaultParty());
        });
    }
}