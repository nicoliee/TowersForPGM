package org.nicolie.towersforpgm.draft;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.MatchManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.start.StartCountdown;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.util.bukkit.Sounds;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
    private final MatchManager matchManager;
    private boolean isDraftActive = false;

    public Draft(Captains captains, AvailablePlayers availablePlayers, Teams teams, LanguageManager languageManager, MatchManager matchManager) {
        this.matchManager = matchManager;
        this.teams = teams;
        this.captains = captains;
        this.availablePlayers = availablePlayers;
        this.languageManager = languageManager;
    }

    public void startDraft(UUID captain1, UUID captain2, Match match) {
        if (matchManager.getMatch() == null){
            matchManager.setCurrentMatch(match);
        }
        cleanLists();
        captains.setCaptain1(captain1);
        captains.setCaptain2(captain2);
        teams.removeFromTeams(match);
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

        match.getCountdown().cancelAll(StartCountdown.class);

        teams.setTeamsSize(0);
        // Decidir aleatoriamente quien empieza el draft
        Random rand = new Random();
        captains.setCaptain1Turn(rand.nextBoolean()); // true o false aleatorio
        isDraftActive = true;
        captains.setMatchWithCaptains(true);

        // Enviar mensaje a todos los jugadores para anunciar los capitanes
        matchManager.getMatch().playSound(Sounds.RAINDROPS);
        SendMessage.broadcast(languageManager.getPluginMessage("captains.captainsHeader"));
        SendMessage.broadcast("&4" + Bukkit.getPlayer(captain1).getName() + " &l&bvs. " + "&9" + Bukkit.getPlayer(captain2).getName());
        SendMessage.broadcast("§m---------------------------------");

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
        suggestPicksForCaptains();
    }

    public void pickPlayer(String username) {
        Player player = Bukkit.getPlayerExact(username);
        String exactUsername = availablePlayers.getExactUser(username);
        if (captains.isCaptain1Turn()) {
            teams.addPlayerToTeam(exactUsername, 1);
            availablePlayers.removePlayer(exactUsername);
            captains.toggleTurn();
            SendMessage.broadcast(languageManager.getConfigurableMessage("captains.choose")
                .replace("{teamcolor}", "&4")
                .replace("{captain}", captains.getCaptain1Name())
                .replace("{player}", exactUsername));
            matchManager.getMatch().playSound(Sounds.MATCH_COUNTDOWN);
        } else {
            teams.addPlayerToTeam(exactUsername, 2);
            availablePlayers.removePlayer(exactUsername);
            teams.assignTeam(player, 2);
            captains.toggleTurn();
            SendMessage.broadcast(languageManager.getConfigurableMessage("captains.choose")
                .replace("{teamcolor}", "&9")
                .replace("{captain}", captains.getCaptain2Name())
                .replace("{player}", exactUsername));
            matchManager.getMatch().playSound(Sounds.MATCH_START);
        }
        // Verificar si el draft ha terminado
        if (availablePlayers.isEmpty()) {
            endDraft();  // Terminar el draft si no hay más jugadores disponibles
            return;
        }
        suggestPicksForCaptains();
    }

    // Método para finalizar el draft
    public void endDraft() {
        if (!isDraftActive) {
            return;
        }

        // Finaliza el draft y muestra los equipos a los capitanes
        isDraftActive = false;

        // Usar el método getAllTeam para obtener todos los jugadores de cada equipo
        Set<String> team1Names = teams.getAllTeam(1); // Obtener todos los jugadores del equipo 1
        Set<String> team2Names = teams.getAllTeam(2); // Obtener todos los jugadores del equipo 2

        // Convertir el conjunto de jugadores a un StringBuilder (como antes) para construir el mensaje
        StringBuilder team1NamesBuilder = new StringBuilder();
        team1Names.forEach(player -> team1NamesBuilder.append(player).append(" "));

        StringBuilder team2NamesBuilder = new StringBuilder();
        team2Names.forEach(player -> team2NamesBuilder.append(player).append(" "));

        String team1Formatted = formatTeamList("§4", team1Names, captains.getCaptain1Name());
        String team2Formatted = formatTeamList("§9", team2Names, captains.getCaptain2Name());
        int team1Size = team1Names.size() + 1; // +1 por el capitán
        int team2Size = team2Names.size() + 1; // +1 por el capitán
        int teamsize = Math.max(team1Size, team2Size); // Tamaño máximo de los equipos

        // Mostrar los equipos
        SendMessage.broadcast(languageManager.getPluginMessage("captains.teamsHeader"));
        SendMessage.broadcast(team1Formatted);
        SendMessage.broadcast("&8[&4" + team1Size + "&8] &l&bvs. " + "&8[&9" + team2Size + "&8]");
        SendMessage.broadcast(team2Formatted);
        SendMessage.broadcast("§m------------------------------");


        // Limpiar jugadores disponibles y resetear tamaño de los equipos
        teams.setTeamsSize(teamsize);

        // Marcar a los capitanes como listos
        captains.setReadyActive(true);
        captains.setMatchWithCaptains(true);

        // Iniciar el juego
        matchManager.getMatch().needModule(StartMatchModule.class).forceStartCountdown(Duration.ofSeconds(60), Duration.ZERO);
    }

    private void suggestPicksForCaptains() {
        List<String> topPlayers = availablePlayers.getTopPlayers();
        if (topPlayers.isEmpty()) {
            return;
        }
        topPlayers = topPlayers.subList(0, Math.min(topPlayers.size(), 3)); // Limitar a los 3 mejores jugadores
        StringBuilder suggestionsBuilder = new StringBuilder();
        for (int i = 0; i < topPlayers.size(); i++) {
            suggestionsBuilder.append(topPlayers.get(i));
            if (i < topPlayers.size() - 2) {
                suggestionsBuilder.append("&8, &b");
            } else if (i == topPlayers.size() - 2) {
                suggestionsBuilder.append(" &8");
                suggestionsBuilder.append(languageManager.getPluginMessage("TowersForPGM.or")).append(" &b");
            }
        }
        String suggestions = languageManager.getConfigurableMessage("captains.suggestions")
            .replace("{suggestions}", suggestionsBuilder.toString());
        if (captains.isCaptain1Turn()) {
            SendMessage.sendToPlayer(Bukkit.getPlayer(captains.getCaptain1()), suggestions);
        } else {
            SendMessage.sendToPlayer(Bukkit.getPlayer(captains.getCaptain2()), suggestions);
        }
    }

// misc
    public void cleanLists() {
        captains.clear();
        availablePlayers.clear();
        teams.clear();
        isDraftActive = false;
    }

    // Función para formatear la lista de nombres según las reglas dadas
    private String formatTeamList(String colorCode, Set<String> names, String captainName) {
        List<String> fullTeam = new ArrayList<>(names);
        fullTeam.add(0, captainName); // Agrega al capitán al inicio

        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < fullTeam.size(); i++) {
            String name = fullTeam.get(i);
            formatted.append(colorCode).append(name);

            if (i < fullTeam.size() - 2) {
                formatted.append("§7, ");
            } else if (i == fullTeam.size() - 2) {
                formatted.append("§7 ").append(languageManager.getPluginMessage("TowersForPGM.and")).append(" ");
            } else {
                formatted.append("§7.");
            }
        }

        return formatted.toString();
    }

// Getters y Setters
    public boolean isDraftActive() {
        return isDraftActive;
    }
}