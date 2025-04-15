package org.nicolie.towersforpgm.draft;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

import tc.oc.pgm.api.match.Match;

import java.util.Random;
import java.util.Set;
import java.util.UUID;

// Draft asume que solo hay dos equipos: "red" y "blue" si no están estos colores tratará de renombrarlos.
// PGM actualmente solo soporta una partida a la vez, por lo solo se realiza un Draft por servidor.

public class Draft {
    private final Captains captains;
    private final AvailablePlayers availablePlayers;
    private final Teams teams;
    private final LanguageManager languageManager;
    private boolean isDraftActive = false;

    public Draft(Captains captains, AvailablePlayers availablePlayers, Teams teams, LanguageManager languageManager) {
        this.teams = teams;
        this.captains = captains;
        this.availablePlayers = availablePlayers;
        this.languageManager = languageManager;
    }

    public void startDraft(UUID captain1, UUID captain2, Match match) {
        cleanLists();
        captains.setCaptain1(captain1);
        captains.setCaptain2(captain2);
        teams.removeFromTeams();
        // Agregar todos los jugadores disponibles (excluyendo a los capitanes)
        for (Player player : Bukkit.getOnlinePlayers()) {
            
            if (player != Bukkit.getPlayer(captain1) && player !=Bukkit.getPlayer(captain2)) {
                String playerName = player.getName();
                availablePlayers.addPlayer(playerName);
            }
        }

        // Agregar a los capitanes a sus respectivos equipos
        teams.assignTeam(Bukkit.getPlayer(captain1), 1);
        teams.assignTeam(Bukkit.getPlayer(captain2), 2);

        if (match.getCountdown() != null) {
            match.getCountdown().cancelAll();
        }
        
        teams.setTeamsSize(0);
        // Decidir aleatoriamente quien empieza el draft
        Random rand = new Random();
        captains.setCaptain1Turn(rand.nextBoolean()); // true o false aleatorio
        isDraftActive = true;
        captains.setMatchWithCaptains(true);

        // Enviar mensaje al capitán que le toca
        if (captains.isCaptain1Turn()) {
            SendMessage.broadcast(languageManager.getConfigurableMessage("captains.turn")
                .replace("{teamcolor}", "&4")
                .replace("{captain}", Bukkit.getPlayer(captains.getCaptain1()).getName()));
        } else {
            SendMessage.broadcast(languageManager.getConfigurableMessage("captains.turn")
                .replace("{teamcolor}", "&9")
                .replace("{captain}", Bukkit.getPlayer(captains.getCaptain2()).getName()));
        }
    }

    public void pickPlayer(String username) {
        Player player = Bukkit.getPlayerExact(username);
        if (captains.isCaptain1Turn()) {
            teams.addPlayerToTeam(username, 1);
            availablePlayers.removePlayer(username);
            teams.assignTeam(player, 1);
            captains.toggleTurn();
            SendMessage.broadcast(languageManager.getConfigurableMessage("captains.choose")
                .replace("{teamcolor}", "&4")
                .replace("{captain}", captains.getCaptain1Name())
                .replace("{player}", username));
            SendMessage.soundBroadcast("note.hat", 1f, 1f);
        } else {
            teams.addPlayerToTeam(username, 2);
            availablePlayers.removePlayer(username);
            teams.assignTeam(player, 2);
            captains.toggleTurn();
            SendMessage.broadcast(languageManager.getConfigurableMessage("captains.choose")
                .replace("{teamcolor}", "&9")
                .replace("{captain}", captains.getCaptain2Name())
                .replace("{player}", username));
                SendMessage.soundBroadcast("note.hat", 1f, 1.5f);
        }

        // Verificar si el draft ha terminado
        if (availablePlayers.isEmpty()) {
            endDraft();  // Terminar el draft si no hay más jugadores disponibles
        }
    }

    // Método para finalizar el draft
    public void endDraft() {
        if (!isDraftActive) {
            return;
        }

        // Finaliza el draft y muestra los equipos a los capitanes
        isDraftActive = false;
        SendMessage.broadcast(languageManager.getPluginMessage("captains.teamsHeader"));

        // Usar el método getAllTeam para obtener todos los jugadores de cada equipo
        Set<String> team1Names = teams.getAllTeam(1); // Obtener todos los jugadores del equipo 1
        Set<String> team2Names = teams.getAllTeam(2); // Obtener todos los jugadores del equipo 2

        // Convertir el conjunto de jugadores a un StringBuilder (como antes) para construir el mensaje
        StringBuilder team1NamesBuilder = new StringBuilder();
        team1Names.forEach(player -> team1NamesBuilder.append(player).append(" "));

        StringBuilder team2NamesBuilder = new StringBuilder();
        team2Names.forEach(player -> team2NamesBuilder.append(player).append(" "));

        int team1Size = team1Names.size() + 1; // +1 por el capitán
        int team2Size = team2Names.size() + 1; // +1 por el capitán
        int teamsize = Math.max(team1Size, team2Size); // Tamaño máximo de los equipos

        // Mostrar los equipos
        SendMessage.broadcast("&4" + captains.getCaptain1Name() + " " + team1NamesBuilder.toString().trim());
        SendMessage.broadcast("&8[&4" + team1Size + "&8] &l&6vs. " + "&8[&9" + team2Size + "&8]");
        SendMessage.broadcast("&9" + captains.getCaptain2Name() + " " + team2NamesBuilder.toString().trim());
        SendMessage.broadcast("§8§m------------------------------");


        // Limpiar jugadores disponibles y resetear tamaño de los equipos
        teams.setTeamsSize(teamsize);

        // Marcar a los capitanes como listos
        captains.setReadyActive(true);

        captains.setMatchWithCaptains(true);

        // Iniciar el juego con un retraso de 60 segundos
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "start 60");
    }
// misc
    public void cleanLists() {
        captains.clear();
        availablePlayers.clear();
        teams.clear();
        isDraftActive = false;
    }

// Getters y Setters
    public boolean isDraftActive() {
        return isDraftActive;
    }
}