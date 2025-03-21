package org.nicolie.towersforpgm.draft;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.SendMessage;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.join.JoinResult;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

// Draft asume que solo hay dos equipos: "red" y "blue" si no están estos colores tratará de renombrarlos.
// PGM actualmente solo soporta una partida a la vez, por lo solo se realiza un Draft por servidor.

public class Draft {
    private final TowersForPGM plugin;
    private List<Player> availablePlayers = new ArrayList<>();
    private List<String> availableOfflinePlayers = new ArrayList<>();
    private Player captain1 = null;
    private Player captain2 = null;
    private List<Player> team1 = new ArrayList<>();
    private List<Player> team2 = new ArrayList<>();
    private List<String> team1Offline = new ArrayList<>();
    private List<String> team2Offline = new ArrayList<>();
    private boolean isDraftActive = false;
    private boolean isCaptain1Turn = true;

    public Draft(TowersForPGM plugin) {
        this.plugin = plugin;
    }

    public void startDraft(Player captain1, Player captain2) {
        cleanLists();
        setCaptain1(captain1);
        setCaptain2(captain2);

        // Agregar todos los jugadores disponibles (excluyendo a los capitanes)
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != captain1 && player != captain2) {
                availablePlayers.add(player);
            }
        }

        // Agregar a los capitanes a sus respectivos equipos
        assignPlayerToTeam(captain1, 1);
        assignPlayerToTeam(captain2, 2);

        Match match = TowersForPGM.getInstance().getCurrentMatch();
        if (match.getCountdown() != null) {
            match.getCountdown().cancelAll();
        }
        

        // Decidir aleatoriamente quien empieza el draft
        Random rand = new Random();
        isCaptain1Turn = rand.nextBoolean(); // Captain 1 empieza si es true
        isDraftActive = true;

        // Enviar mensaje al capitán que le toca
        if (isCaptain1Turn) {
            SendMessage.broadcast(plugin.getConfigurableMessage("captains.turn")
                .replace("{teamcolor}", "&4")
                .replace("{captain}", captain1.getName()));
        } else {
            SendMessage.broadcast(plugin.getConfigurableMessage("captains.turn")
                .replace("{teamcolor}", "&9")
                .replace("{captain}", captain2.getName()));
        }
    }

    public void addToDraft(String username){
        Player player = Bukkit.getPlayer(username);
        if(player == null){
            addOfflinePlayerToDraft(username);
        } else {
            addOnlinePlayerToDraft(player);
        }
    }

    public void removeFromDraft(String username){
        Player player = Bukkit.getPlayer(username);
        if(player == null){
            deleteOfflinePLayerFromDraft(username);
        } else {
            deteleOnlinePlayerFromDraft(player);
        }
        // Verificar si el draft ha terminado
        if (availablePlayers.isEmpty() && availableOfflinePlayers.isEmpty()) {
            endDraft();  // Terminar el draft si no hay más jugadores disponibles
        }
    }

    public void pickPlayer(String username) {
        Player player = Bukkit.getPlayer(username);

        if (player == null) {
            // El jugador está offline, se agrega al equipo offline
            if (isCaptain1Turn) {
                team1Offline.add(username);
                SendMessage.broadcast(plugin.getConfigurableMessage("captains.choose")
                    .replace("{teamcolor}", "&4")
                    .replace("{captain}", captain1.getName())
                    .replace("{player}", username));
            } else {
                team2Offline.add(username);
                SendMessage.broadcast(plugin.getConfigurableMessage("captains.choose")
                    .replace("{teamcolor}", "&9")
                    .replace("{captain}", captain2.getName())
                    .replace("{player}", username));
            }
            availableOfflinePlayers.remove(username);  // Remover el jugador de la lista de disponibles
        } else {
            // El jugador está online, se agrega al equipo correspondiente
            if (isCaptain1Turn) {
                team1.add(player);
                SendMessage.broadcast(plugin.getConfigurableMessage("captains.choose")
                    .replace("{teamcolor}", "&4")
                    .replace("{captain}", captain1.getName())
                    .replace("{player}", player.getName()));
                assignPlayerToTeam(player, 1);
            } else {
                team2.add(player);
                SendMessage.broadcast(plugin.getConfigurableMessage("captains.choose")
                    .replace("{teamcolor}", "&9")
                    .replace("{captain}", captain2.getName())
                    .replace("{player}", player.getName()));
                assignPlayerToTeam(player, 2);
            }
            availablePlayers.remove(player);  // Remover el jugador de la lista de disponibles
        }

        // Verificar si el draft ha terminado
        if (availablePlayers.isEmpty() && availableOfflinePlayers.isEmpty()) {
            endDraft();  // Terminar el draft si no hay más jugadores disponibles
        }
    }

    public void endDraft() {
        if(!isDraftActive){
            return;
        }
        // Finaliza el draft y muestra los equipos a los capitanes
        isDraftActive = false;
        SendMessage.broadcast(plugin.getPluginMessage("captains.teamsHeader"));

        StringBuilder team1Names = new StringBuilder();
        for (Player player : team1) {
            team1Names.append(player.getName()).append(" ");
        }

        StringBuilder team2Names = new StringBuilder();
        for (Player player : team2) {
            team2Names.append(player.getName()).append(" ");
        }
        if (!team1Offline.isEmpty()) {
            for (String player : team1Offline) {
                team1Names.append(player).append(" ");
            }
        }
        if (!team2Offline.isEmpty()) {
            for (String player : team2Offline) {
                team2Names.append(player).append(" ");
            }
        }
        int Team1 = getTeam1().size() + getTeam1Offline().size() + 1;
        int Team2 = getTeam2().size() + getTeam2Offline().size() + 1;
        int teamSize = Math.max(Team1, Team2);
        SendMessage.broadcast("&4" + captain1.getName() + " " + team1Names.toString().trim());
        SendMessage.broadcast("&8[&4"+String.valueOf(Team1)+"&8] &l&6vs. " + "&8[&9"+String.valueOf(Team2)+"&8]");
        SendMessage.broadcast("&9" + captain2.getName() + " " + team2Names.toString().trim());
        SendMessage.broadcast("------------------------------");
        availableOfflinePlayers.clear();
        availablePlayers.clear();
        setTeamsSize(teamSize);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "start 60");
    }

    private void addOnlinePlayerToDraft(Player player) {
        if (!isDraftActive) {
            return;
        }

        // Verificar que el jugador no sea un capitán o ya esté en un equipo
        if (player.equals(captain1) || player.equals(captain2) || team1.contains(player) || team2.contains(player)) {
            SendMessage.sendToPlayer(player, plugin.getPluginMessage("captains.alreadyInTeam"));
            return;
        }

        if (!availablePlayers.contains(player)) {
            availablePlayers.add(player);
            SendMessage.broadcast(plugin.getConfigurableMessage("picks.add")
                .replace("{player}", player.getName()));
        } else {
            SendMessage.sendToPlayer(player, plugin.getPluginMessage("captains.alreadyInDraft"));
        }
    }

    private void deteleOnlinePlayerFromDraft(Player player) {
        // Verificar que el jugador no sea un capitán o ya esté en un equipo
        if (player.equals(captain1) || player.equals(captain2) || team1.contains(player) || team2.contains(player)) {
            SendMessage.sendToPlayer(player, plugin.getPluginMessage("captains.alreadyInTeam"));
            return;
        }

        if (availablePlayers.contains(player)) {
            availablePlayers.remove(player);
            SendMessage.broadcast(plugin.getConfigurableMessage("picks.remove")
                .replace("{player}", player.getName()));
        } else {
            SendMessage.sendToPlayer(player, plugin.getPluginMessage("captains.notInDraft"));
        }
    }

    private void addOfflinePlayerToDraft(String username){
        if (!isDraftActive) {
            return;
        }
        if(username.equals(captain1.getName()) || username.equals(captain2.getName()) || team1Offline.contains(username) || team2Offline.contains(username)){
            SendMessage.sendToAdmins(plugin.getPluginMessage("captains.alreadyInTeam"));
            return;
        }
        if(!availableOfflinePlayers.contains(username)){
            availableOfflinePlayers.add(username);
            SendMessage.broadcast(plugin.getConfigurableMessage("picks.add")
                .replace("{player}", username));
        } else {
            SendMessage.sendToAdmins(plugin.getPluginMessage("captains.alreadyInDraft"));
        }
    }

    private void deleteOfflinePLayerFromDraft(String username){
        if (!isDraftActive) {
            return;
        }
        if(availableOfflinePlayers.contains(username)){
            availableOfflinePlayers.remove(username);
            SendMessage.broadcast(plugin.getConfigurableMessage("picks.remove")
                .replace("{player}", username));
        } else {
            SendMessage.sendToAdmins(plugin.getPluginMessage("captains.notInDraft"));
        }
    }
// misc
    public void cleanLists() {
        availablePlayers.clear();
        availableOfflinePlayers.clear();
        team1.clear();
        team2.clear();
        team1Offline.clear();
        team2Offline.clear();
        captain1 = null;
        captain2 = null;
        endDraft();
    }

    public void assignPlayerToTeam(Player player, int teamNumber) {
        Match match = TowersForPGM.getInstance().getCurrentMatch();
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

    private void setTeamsSize(int teamSize) {
        Match match = TowersForPGM.getInstance().getCurrentMatch();
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

// Getters y Setters
    public void toggleTurn() {
        isCaptain1Turn = !isCaptain1Turn;
    }

    public boolean isDraftActive() {
        return isDraftActive;
    }
    
    public Player getCaptain1() {
        return captain1;
    }

    public void setCaptain1(Player captain1) {
        this.captain1 = captain1;
    }

    public String getCaptain1Name() {
        return captain1.getName();
    }

    public Player getCaptain2() {
        return captain2;
    }

    public void setCaptain2(Player captain2) {
        this.captain2 = captain2;
    }

    public String getCaptain2Name() {
        return captain2.getName();
    }

    public boolean isCaptain1Turn() {
        return isCaptain1Turn;
    }

    public List<Player> getAvailablePlayers() {
        return availablePlayers;
    }

    public List<String> getAvailablePlayerNames() {
        return availablePlayers.stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    public List<Player> getTeam1() {
        return team1;
    }

    public List<String> getTeam1Names(){
        return team1.stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    public List<Player> getTeam2() {
        return team2;
    }

    public List<String> getTeam2Names(){
        return team2.stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    public List<String> getTeam1Offline() {
        return team1Offline;
    }

    public List<String> getTeam2Offline() {
        return team2Offline;
    }

    public List<String> getAvailableOfflinePlayers() {
        return availableOfflinePlayers;
    }
}