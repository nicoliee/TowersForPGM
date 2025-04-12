package org.nicolie.towersforpgm.listeners;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.SendMessage;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchStatsEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.stats.StatsMatchModule;

public class MatchStatsListener implements Listener {
    private final TowersForPGM plugin = TowersForPGM.getInstance();
    
    @EventHandler
    public void onMatchStatsEvent(MatchStatsEvent event) {
        Match match = event.getMatch();
        ScoreMatchModule scoreMatchModule = match.getModule(ScoreMatchModule.class);
        StatsMatchModule statsModule = match.getModule(StatsMatchModule.class);
        
        List<MatchPlayer> allPlayers = new ArrayList<>(match.getParticipants());
        allPlayers.addAll(plugin.getDisconnectedPlayers().values());
        
        Map<Integer, List<String>> killsMap = new TreeMap<>(Collections.reverseOrder());
        Map<Integer, List<String>> pointsMap = new TreeMap<>(Collections.reverseOrder());
        
        for (MatchPlayer player : allPlayers) {
            PlayerStats playerStats = statsModule != null ? statsModule.getPlayerStat(player) : null;
            int kills = playerStats != null ? playerStats.getKills() : 0;
            int totalPoints = scoreMatchModule != null ? (int) scoreMatchModule.getContribution(player.getId()) : 0;
            
            String playerName = (player.getParty() != null && player.getParty().getColor() != null ? player.getParty().getColor().toString() : "§3") + player.getNameLegacy();
            
            killsMap.computeIfAbsent(kills, k -> new ArrayList<>()).add(playerName);
            if (totalPoints > 0) {
                pointsMap.computeIfAbsent(totalPoints, k -> new ArrayList<>()).add(playerName);
            }
        }
        
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!killsMap.isEmpty()) {
                SendMessage.sendToWorld(event.getWorld().getName(), "&6 ---------- Top Kills: ---------- ");
                
                // Ordenamos killsMap por el número de kills en orden descendente
                killsMap.entrySet().stream()
                    .sorted((entry1, entry2) -> Integer.compare(entry2.getKey(), entry1.getKey())) // Ordenamos de mayor a menor
                    .limit(5) // Limita a las primeras 5 líneas
                    .forEach(entry -> {
                        String formattedNames = formatPlayerNames(entry.getValue());
                        SendMessage.sendToWorld(event.getWorld().getName(), "&6" + formattedNames + " - " + entry.getKey());
                    });
            }
        }, 5L);
             
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!pointsMap.isEmpty()) {
                SendMessage.sendToWorld(event.getWorld().getName(), "&b --------- Top Puntos: --------- ");
                int position = 1;
                for (Map.Entry<Integer, List<String>> entry : pointsMap.entrySet()) {
                    String formattedNames = formatPlayerNames(entry.getValue());
                    SendMessage.sendToWorld(event.getWorld().getName(), "&b" + position + ". " + formattedNames + " - " + entry.getKey());
                    position++;
                }
            }
        }, 10L);
    }
    
    private String formatPlayerNames(List<String> names) {
        if (names.size() == 1) {
            return names.get(0);
        }
        if (names.size() == 2) {
            return names.get(0) + " &8and " + names.get(1);
        }
        return String.join("&8, ", names.subList(0, names.size() - 1)) + " &8and " + names.get(names.size() - 1);
    }
}