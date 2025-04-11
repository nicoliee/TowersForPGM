package org.nicolie.towersforpgm.listeners;

import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.stats.StatsMatchModule;
import tc.oc.pgm.stats.PlayerStats;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.Stats;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.preparationTime.TorneoListener;

import java.util.ArrayList;
import java.util.List;

public class MatchFinishListener implements Listener {
    private final TowersForPGM plugin;
    private final TorneoListener torneoListener;
    private final RefillManager refillManager;
    private final Draft draft;

    public MatchFinishListener(TowersForPGM plugin, TorneoListener torneoListener, RefillManager refillManager, Draft draft) {
        this.plugin = plugin;
        this.torneoListener = torneoListener;
        this.refillManager = refillManager;
        this.draft = draft;
    }
    
    @EventHandler
    public void onMatchFinish(MatchFinishEvent event) {
        Match match = event.getMatch();
        String worldName = event.getMatch().getWorld().getName();
        String mapName = event.getMatch().getMap().getName();
        torneoListener.stopProtection(null, worldName);
        refillManager.clearWorldData(worldName);
        draft.cleanLists();
        ScoreMatchModule scoreMatchModule = match.getModule(ScoreMatchModule.class);
        StatsMatchModule statsModule = match.getModule(StatsMatchModule.class);

        if (plugin.getIsDatabaseActivated() && ConfigManager.getTableForMap(mapName) != "none") {
            if (scoreMatchModule != null && statsModule != null) {
                List<Stats> playerStatsList = new ArrayList<>();
        
                // Crear una lista única con todos los jugadores (conectados y desconectados)
                List<MatchPlayer> allPlayers = new ArrayList<>();
                allPlayers.addAll(event.getMatch().getParticipants());
                allPlayers.addAll(plugin.getDisconnectedPlayers().values());
        
                // Recorrer todos los jugadores
                for (MatchPlayer player : allPlayers) {
                    // Obtenemos estadísticas del jugador
                    PlayerStats playerStats = statsModule.getPlayerStat(player);
                    int totalPoints = (int) scoreMatchModule.getContribution(player.getId());
                    boolean isWinner = event.getMatch().getWinners().contains(player.getCompetitor());
        
                    playerStatsList.add(new Stats(
                            player.getNameLegacy(),
                            playerStats != null ? playerStats.getKills() : 0,
                            playerStats != null ? playerStats.getDeaths() : 0,
                            playerStats != null ? playerStats.getAssists() : 0,
                            totalPoints,
                            isWinner ? 1 : 0,
                            1
                    ));
                };
                // Si hay estadísticas que enviar, realizar la actualización
                if (!playerStatsList.isEmpty()) {
                    // Aquí envías las estadísticas a la base de datos o lo que sea necesario
                    StatsManager.updateStats(ConfigManager.getTableForMap(mapName), playerStatsList);
                    System.out.println("[+] " + mapName + " en la tabla: " + ConfigManager.getTableForMap(mapName) + " con " + playerStatsList.size() + " jugadores.");
                    System.out.println("Stats: " + playerStatsList.toString());
                    //TODO: Enviar mensaje a los desarrolladores configurado con language.yml
                    SendMessage.sendToDevelopers("&a[+] " + mapName + " en la tabla: " + ConfigManager.getTableForMap(mapName) + " con " + playerStatsList.size() + " jugadores.");
                }
            }
        }        
    }
}