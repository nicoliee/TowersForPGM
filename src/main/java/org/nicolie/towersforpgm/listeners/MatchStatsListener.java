package org.nicolie.towersforpgm.listeners;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchStatsEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.stats.StatsMatchModule;

public class MatchStatsListener implements Listener {
    private final TowersForPGM plugin = TowersForPGM.getInstance();
    private final LanguageManager languageManager;

    public MatchStatsListener(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    
    @EventHandler
    public void onMatchStatsEvent(MatchStatsEvent event) {
        Match match = event.getMatch();
        Collection<Gamemode> gamemodes = match.getMap().getGamemodes();
        if (!gamemodes.contains(Gamemode.SCOREBOX)) {
            return;
        }
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
            
            String playerName;
            if (player.getParty() == null) {
                playerName = "§3" + player.getNameLegacy();
            } else {
                playerName = player.getPrefixedName();
            }
            
            killsMap.computeIfAbsent(kills, k -> new ArrayList<>()).add(playerName);
            if (totalPoints > 0) {
                pointsMap.computeIfAbsent(totalPoints, k -> new ArrayList<>()).add(playerName);
            }
        }
        
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!killsMap.isEmpty()) {
                SendMessage.sendToWorld(event.getWorld().getName(), "&m--------------------§r &6Top Killers &f&m--------------------");
                int position = 1;
                List<Map.Entry<Integer, List<String>>> sortedKills = new ArrayList<>(killsMap.entrySet());
                sortedKills.sort((entry1, entry2) -> Integer.compare(entry2.getKey(), entry1.getKey()));
                
                for (int i = 0; i < Math.min(5, sortedKills.size()); i++) { // Limita a las primeras 5 líneas
                    Map.Entry<Integer, List<String>> entry = sortedKills.get(i);
                    String formattedNames = formatPlayerNames(entry.getValue());
                    SendMessage.sendToWorld(event.getWorld().getName(), "&6" + position + ". " + formattedNames + " - " + entry.getKey());
                    position++;
                }
            }
        }, 5L);
             
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!pointsMap.isEmpty()) {
                SendMessage.sendToWorld(event.getWorld().getName(), "&m--------------------§r &bTop Scorer &f&m-------------------");
                int position = 1;
                for (Map.Entry<Integer, List<String>> entry : pointsMap.entrySet()) {
                    String formattedNames = formatPlayerNames(entry.getValue());
                    SendMessage.sendToWorld(event.getWorld().getName(), "&b" + position + ". " + formattedNames + " - " + entry.getKey());
                    position++;
                }
            }
        }, 10L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (statsModule != null) {
                double bestRatio = -1;
                String mvpName = null;
                for (MatchPlayer player : allPlayers) {
                    PlayerStats stats = statsModule.getPlayerStat(player);
                    double damageDone = stats != null ? stats.getDamageDone() : 0;
                    double damageTaken = stats != null ? stats.getDamageTaken() : 0;
                    double ratio = damageTaken > 0 ? damageDone / damageTaken : (damageDone > 0 ? damageDone : 0);

                    if (ratio > bestRatio) {
                        bestRatio = ratio;
                        if (player.getParty() == null) {
                            mvpName = "§3" + player.getNameLegacy();
                        } else {
                            mvpName = player.getPrefixedName();
                        }
                    }
                }
                if (mvpName != null) {
                    Component mvpmessage = Component.text(languageManager.getPluginMessage("stats.mvp")
                            .replace("{player}", mvpName));
                    match.sendMessage(mvpmessage);
                }
            }
        }, 15L);
    }
    
    private String formatPlayerNames(List<String> names) {
        if (names.size() == 1) {
            return names.get(0);
        }
        if (names.size() == 2) {
            return names.get(0) + " &8" + languageManager.getPluginMessage("TowersForPGM.and") + " " + names.get(1);
        }
        return String.join("&8, ", names.subList(0, names.size() - 1)) + " &8" + languageManager.getPluginMessage("TowersForPGM.and") + " " + names.get(names.size() - 1);
    }
}