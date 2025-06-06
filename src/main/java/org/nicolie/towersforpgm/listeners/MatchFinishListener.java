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
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.database.Stats;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;

import java.util.ArrayList;
import java.util.List;

public class MatchFinishListener implements Listener {
    private final TowersForPGM plugin;
    private final LanguageManager languageManager;
    private final PreparationListener preparationListener;
    private final RefillManager refillManager;
    private final Draft draft;

    public MatchFinishListener(TowersForPGM plugin, PreparationListener preparationListener, RefillManager refillManager,
            Draft draft, LanguageManager languageManager) {
        this.plugin = plugin;
        this.preparationListener = preparationListener;
        this.refillManager = refillManager;
        this.draft = draft;
        this.languageManager = languageManager;
    }

    @EventHandler
    public void onMatchFinish(MatchFinishEvent event) {
        // Detener eventos de protección, refill y draft
        preparationListener.stopProtection(null, event.getMatch());
        refillManager.clearWorldData(event.getMatch().getWorld().getName());
        draft.cleanLists();

        // Estadísticas
        if (plugin.isStatsCancel()) {
            cancelStats(event);
        }else{
            matchStats(event);
        }
    }

    private void cancelStats(MatchFinishEvent event) {
        String mapName = event.getMatch().getMap().getName();
        System.out.println("[-] Stats cancelled for match-" + event.getMatch().getId() + ": "
                                + mapName + ", stats not sent to database.");
        SendMessage.sendToDevelopers(languageManager.getPluginMessage("stats.consoleCancel")
                .replace("{id}", String.valueOf(event.getMatch().getId()))
                .replace("{map}", mapName)
                .replace("{size}", String.valueOf(event.getMatch().getParticipants().size())));
        plugin.setStatsCancel(false);
    }

    private void matchStats(MatchFinishEvent event) {
        Match match = event.getMatch();
        String mapName = match.getMap().getName();
        ScoreMatchModule scoreMatchModule = match.getModule(ScoreMatchModule.class);
        StatsMatchModule statsModule = match.getModule(StatsMatchModule.class);

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
                        playerStats != null ? ((playerStats.getDamageDone() + playerStats.getBowDamage()) / 2) : 0,
                        playerStats != null ? ((playerStats.getDamageTaken() + playerStats.getBowDamageTaken()) / 2)
                                : 0,
                        totalPoints,
                        isWinner ? 1 : 0,
                        1));
            }
            // Si hay estadísticas que enviar, realizar la actualización
            if (!playerStatsList.isEmpty()) {
                String table;
                if (ConfigManager.getTempTable() != null) {
                    table = ConfigManager.getTempTable();
                    ConfigManager.removeTempTable();
                } else {
                    table = ConfigManager.getTableForMap(mapName);
                } 
                // Aquí envías las estadísticas a la base de datos o lo que sea necesario
                StatsManager.updateStats(table, playerStatsList);
                System.out.println("[+] match-" + event.getMatch().getId() + ": " + mapName
                        + ", stats on table " + table + ": " + playerStatsList.toString());
                SendMessage.sendToDevelopers(languageManager.getPluginMessage("stats.console")
                        .replace("{id}", String.valueOf(event.getMatch().getId()))
                        .replace("{map}", mapName)
                        .replace("{table}", table)
                        .replace("{size}", String.valueOf(playerStatsList.size())));
            }
        }
    }
}