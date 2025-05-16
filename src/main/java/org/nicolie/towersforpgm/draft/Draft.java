package org.nicolie.towersforpgm.draft;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.nicolie.towersforpgm.MatchManager;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.start.StartCountdown;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.util.bukkit.Sounds;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.nicolie.towersforpgm.draft.events.DraftEndEvent;
import org.nicolie.towersforpgm.draft.events.DraftStartEvent;

public class Draft {
    private final TowersForPGM plugin = TowersForPGM.getInstance(); 
    private final Captains captains;
    private final AvailablePlayers availablePlayers;
    private final Teams teams;
    private final LanguageManager languageManager;
    private final MatchManager matchManager;
    private final Utilities utilities;
    private static boolean isDraftActive = false;
    private BukkitRunnable draftTimer;
    private BossBar pickTimerBar;
    
    // Custom draft order pattern variables
    private String customOrderPattern = ""; // Ejemplo: "ABABAB"
    // 'A' significa turno del capitán que fue primero, 'B' significa turno del otro capitán
    // Se usa para determinar el orden de los turnos
    private int customOrderMinPlayers = 6; // Número mínimo de jugadores para usar el patrón
    private int currentPatternIndex = 0; // Índice actual del patrón
    private boolean usingCustomPattern = false; // Indica si se está usando un patrón personalizado
    private boolean firstCaptainTurn; // Indica quién fue el primer capitán en pickear

    public Draft(Captains captains, AvailablePlayers availablePlayers, Teams teams, LanguageManager languageManager,
            MatchManager matchManager, Utilities utilities) {
        this.matchManager = matchManager;
        this.teams = teams;
        this.captains = captains;
        this.availablePlayers = availablePlayers;
        this.languageManager = languageManager;
        this.utilities = utilities;
    }

    public void setCustomOrderPattern(String pattern, int minPlayers) {
        if (pattern != null && !pattern.isEmpty()) {
            this.customOrderPattern = pattern.toUpperCase();
            this.customOrderMinPlayers = minPlayers;
        }
    }

    public void startDraft(UUID captain1, UUID captain2, List<MatchPlayer> players, Match match) {
        if (matchManager.getMatch() == null) {
            matchManager.setCurrentMatch(match);
        }
        cleanLists();
        isDraftActive = true;
        captains.setCaptain1(captain1);
        captains.setCaptain2(captain2);
        teams.removeFromTeams(match);

        teams.addPlayerToTeam(Bukkit.getPlayer(captain1).getName(), 1);
        teams.addPlayerToTeam(Bukkit.getPlayer(captain2).getName(), 2);

        // Agregar jugadores disponibles a la lista
        for (MatchPlayer player : players) {
            availablePlayers.addPlayer(player.getNameLegacy());
        }
        // Agregar a los capitanes a sus respectivos equipos
        teams.assignTeam(Bukkit.getPlayer(captain1), 1);
        teams.assignTeam(Bukkit.getPlayer(captain2), 2);

        match.getCountdown().cancelAll(StartCountdown.class);
        teams.setTeamsSize(0);
        Random rand = new Random();
        captains.setCaptain1Turn(rand.nextBoolean());

        // Guardar quién fue el primer capitán en pickear (será el "A" en el patrón)
        firstCaptainTurn = captains.isCaptain1Turn();
        
        // Revisar si se debe usar un patrón de orden personalizado
        if (!customOrderPattern.isEmpty() && availablePlayers.getAllAvailablePlayers().size() >= customOrderMinPlayers) {
            usingCustomPattern = true;
            currentPatternIndex = 0;
        } else {
            usingCustomPattern = false;
            customOrderPattern = ""; // Reiniciar el patrón si no se usa
            currentPatternIndex = 0; // Reiniciar el índice del patrón
        }
        
        captains.setMatchWithCaptains(true);

        // Enviar mensaje a todos los jugadores para anunciar los capitanes
        matchManager.getMatch().playSound(Sounds.RAINDROPS);
        SendMessage.broadcast(languageManager.getPluginMessage("captains.captainsHeader"));
        SendMessage.broadcast("&4" + Bukkit.getPlayer(captain1).getName() + " &l&bvs. " + "&9"
                + Bukkit.getPlayer(captain2).getName());
        SendMessage.broadcast("§m---------------------------------");

        // Enviar mensaje al capitán que le toca
        String teamColor = captains.isCaptain1Turn() ? "&4" : "&9";
        UUID captainUUID = captains.isCaptain1Turn() ? captains.getCaptain1() : captains.getCaptain2();
        String captainName = Bukkit.getPlayer(captainUUID).getName();
        SendMessage.broadcast(languageManager.getConfigurableMessage("captains.turn")
                .replace("{teamcolor}", teamColor)
                .replace("{captain}", captainName));
        plugin.giveitem();
        startDraftTimer();

        // Trigger DraftStartEvent
        Bukkit.getPluginManager().callEvent(new DraftStartEvent());
    }
    public void startDraft(UUID captain1, UUID captain2, List<MatchPlayer> availablePlayers) {

    }
    public void startDraftTimer() {
        if (!ConfigManager.isDraftTimer()) {
            return;
        }

        if (pickTimerBar == null) {
            // Crear BossBar personalizada solo si no existe
            String currentCaptainName = captains.isCaptain1Turn() ? captains.getCaptain1Name() : captains.getCaptain2Name();
            String currentCaptainColor = captains.isCaptain1Turn() ? "§4" : "§9";
            String bossbarMessage = languageManager.getPluginMessage("captains.bossbar")
                    .replace("{captain}", currentCaptainColor + currentCaptainName);

            pickTimerBar = BossBar.bossBar(
                    Component.text(bossbarMessage.replace("{time}", String.valueOf(utilities.timerDuration()))),
                    1f,
                    BossBar.Color.YELLOW,
                    BossBar.Overlay.PROGRESS);
            matchManager.getMatch().showBossBar(pickTimerBar);
        }

        if (draftTimer != null) {
            draftTimer.cancel();
        }

        int initialTime = utilities.timerDuration();
        int[] timeLeft = { initialTime };

        // Actualizar el mensaje de la BossBar antes de iniciar el Runnable
        String currentCaptainName = captains.isCaptain1Turn() ? captains.getCaptain1Name() : captains.getCaptain2Name();
        String currentCaptainColor = captains.isCaptain1Turn() ? "§4" : "§9";
        String bossbarMessage = languageManager.getPluginMessage("captains.bossbar")
                .replace("{captain}", currentCaptainColor + currentCaptainName);

        draftTimer = new BukkitRunnable() {
            @Override
            public void run() {
                // Actualizar solo los segundos en la BossBar
                float progress = Math.max(0f, (float) timeLeft[0] / initialTime);
                pickTimerBar.name(Component.text(bossbarMessage.replace("{time}", SendMessage.formatTime(timeLeft[0]))));
                pickTimerBar.progress(progress);

                // Sugerencias, sonidos y mensajes
                if (timeLeft[0] == initialTime - 5) {
                    utilities.suggestPicksForCaptains();
                }
                if (timeLeft[0] <= 30 && timeLeft[0] > 3) {
                    MatchPlayer currentCaptain = PGM.get().getMatchManager().getPlayer(captains.getCurrentCaptain());
                    currentCaptain.playSound(Sounds.INVENTORY_CLICK);
                } else if (timeLeft[0] <= 3 && timeLeft[0] >= 1) {
                    MatchPlayer currentCaptain = PGM.get().getMatchManager().getPlayer(captains.getCurrentCaptain());
                    currentCaptain.playSound(Sounds.WARNING);
                }
                if ((timeLeft[0] <= 5 && timeLeft[0] >= 1)) {
                    MatchPlayer currentCaptain = PGM.get().getMatchManager().getPlayer(captains.getCurrentCaptain());
                    SendMessage.sendToPlayer(currentCaptain.getBukkit(),
                            languageManager.getConfigurableMessage("captains.timeRemaining").replace("{time}",
                                    SendMessage.formatTime(timeLeft[0])));
                }

                // Si el tiempo se acaba, el capitán elige un jugador aleatorio
                if (timeLeft[0] == 0) {
                    if (utilities.randomPick() == null) {
                        endDraft();
                    } else {
                        pickPlayer(utilities.randomPick());
                        this.cancel();
                    }
                }
                timeLeft[0]--;
            }
        };
        draftTimer.runTaskTimer(TowersForPGM.getInstance(), 0, 20); // cada segundo
    }

    public void pickPlayer(String username) {
        Player player = Bukkit.getPlayerExact(username);
        String exactUsername = availablePlayers.getExactUser(username);
        String teamColor = captains.isCaptain1Turn() ? "§4" : "§9";
        String captainName = captains.isCaptain1Turn() ? captains.getCaptain1Name() : captains.getCaptain2Name();
        Sound sound = captains.isCaptain1Turn() ? Sounds.MATCH_COUNTDOWN : Sounds.MATCH_START;
        int teamNumber = captains.isCaptain1Turn() ? 1 : 2;

        teams.addPlayerToTeam(exactUsername, teamNumber);
        availablePlayers.removePlayer(exactUsername);
        teams.assignTeam(player, teamNumber);
        plugin.giveItem(player);

        SendMessage.broadcast(languageManager.getConfigurableMessage("captains.choose")
                .replace("{teamcolor}", teamColor)
                .replace("{captain}", captainName)
                .replace("{player}", exactUsername));
        matchManager.getMatch().playSound(sound);

        // Manejar el orden de los turnos según el patrón personalizado o alternado
        updateTurnOrder();

        // Verificar si el draft ha terminado
        if (availablePlayers.isEmpty()) {
            endDraft(); // Terminar el draft si no hay más jugadores disponibles
            return;
        }
        // Reiniciar el temporizador al pickear un jugador
        plugin.updateInventories();
        startDraftTimer();
    }
    
    private void updateTurnOrder() {
        if (usingCustomPattern) {
            currentPatternIndex++;
            
            // Si hemos llegado al final del patrón, alternamos el turno
            if (currentPatternIndex >= customOrderPattern.length()) {
                usingCustomPattern = false;
                captains.toggleTurn();
            } else {
                // Obtener el siguiente capitán según el patrón
                char nextCaptain = customOrderPattern.charAt(currentPatternIndex);
                boolean shouldBeCaptain1Turn;
                
                if (nextCaptain == 'A') {
                    // Si es 'A', debe ser el turno del capitán que pickeo primero
                    shouldBeCaptain1Turn = firstCaptainTurn;
                } else {
                    // Si es 'B', debe ser el turno del otro capitán
                    shouldBeCaptain1Turn = !firstCaptainTurn;
                }
                // Si un capitán debe pickear seguido enviar un mensaje
                if ((shouldBeCaptain1Turn == captains.isCaptain1Turn()) || (!shouldBeCaptain1Turn == !captains.isCaptain1Turn())) {
                    String message = languageManager.getConfigurableMessage("captains.turn")
                        .replace("{teamcolor}", captains.isCaptain1Turn() ? "&4" : "&9")
                        .replace("{captain}", Bukkit.getPlayer(captains.isCaptain1Turn() ? captains.getCaptain1() 
                                    : captains.getCaptain2()).getName());
                    SendMessage.broadcast(message);
                }
                captains.setCaptain1Turn(shouldBeCaptain1Turn);
            }
        } else {
            // Alternar el turno de los capitanes si no estamos usando un patrón
            captains.toggleTurn();
        }
    }

    // Método para finalizar el draft
    public void endDraft() {
        if (draftTimer != null) {
            draftTimer.cancel();
        }

        if (!isDraftActive) {
            return;
        }

        // Finaliza el draft y muestra los equipos a los capitanes
        isDraftActive = false;

        // cancelar timer
        if (draftTimer != null) {
            draftTimer.cancel();
        }

        // Usar el método getAllTeam para obtener todos los jugadores de cada equipo
        List<String> team1Names = new ArrayList<>(teams.getAllTeam(1));
        List<String> team2Names = new ArrayList<>(teams.getAllTeam(2));

        StringBuilder team1 = utilities.buildLists(team1Names, "§4", false);
        StringBuilder team2 = utilities.buildLists(team2Names, "§9", false);
        int team1Size = team1Names.size();
        int team2Size = team2Names.size();
        int teamsize = Math.max(team1Size, team2Size); // Tamaño máximo de los equipos

        // Mostrar los equipos
        SendMessage.broadcast(languageManager.getPluginMessage("captains.teamsHeader"));
        SendMessage.broadcast(team1.toString());
        SendMessage.broadcast("&8[&4" + team1Size + "&8] &l&bvs. " + "&8[&9" + team2Size + "&8]");
        SendMessage.broadcast(team2.toString());
        SendMessage.broadcast("§m------------------------------");

        // Limpiar jugadores disponibles y resetear tamaño de los equipos
        teams.setTeamsSize(teamsize);

        // Marcar a los capitanes como listos
        captains.setReadyActive(true);
        captains.setMatchWithCaptains(true);

        // Quitar bossbar
        removeBossbar();
        // Iniciar el juego
        matchManager.getMatch().needModule(StartMatchModule.class).forceStartCountdown(Duration.ofSeconds(90),
                Duration.ZERO);

        // Trigger DraftEndEvent
        Bukkit.getPluginManager().callEvent(new DraftEndEvent());
    }

    // misc
    public void cleanLists() {
        captains.clear();
        availablePlayers.clear();
        teams.clear();
        isDraftActive = false;
        usingCustomPattern = false;
        currentPatternIndex = 0;
        if (draftTimer != null) {
            draftTimer.cancel();
        }
    }

    private void removeBossbar() {
        if (pickTimerBar != null) {
            matchManager.getMatch().hideBossBar(pickTimerBar);
            pickTimerBar = null;
        }
    }

    // Getters y Setters
    public static boolean isDraftActive() {
        return isDraftActive;
    }
}