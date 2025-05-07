package org.nicolie.towersforpgm.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;

import tc.oc.pgm.api.party.event.CompetitorScoreChangeEvent;
import tc.oc.pgm.score.ScoreCause;

// Sonido de puntuación imitando el plugin de The Towers

public class CompetitorScoreChangeListener implements Listener {
    @EventHandler
    public void onPlayerScore(CompetitorScoreChangeEvent event) {
        if (event.getCause() == ScoreCause.SCOREBOX) {
            String team = event.getCompetitor().getParty().getDefaultName();
            Bukkit.getScheduler().runTaskLater(TowersForPGM.getInstance(), () -> {
                event.getMatch().getPlayers().forEach(p -> {
                    if (p == null || p.getBukkit() == null || p.getParty() == null) {
                        return;
                    }
                    boolean sameTeam = p.getParty().getDefaultName().equals(team);
                    boolean isObserving = p.isObserving();
                    Player playerBukkit = p.getBukkit();
                    
                    if (sameTeam || isObserving) {
                        try {
                            playerBukkit.playSound(p.getLocation(), Sound.valueOf("ENTITY_EXPERIENCE_ORB_PICKUP"), 1.0f, 2.0f);
                        } catch (IllegalArgumentException e) {
                            playerBukkit.playSound(p.getLocation(), Sound.valueOf("SUCCESSFUL_HIT"), 1.0f, 2.0f);
                        }
                    } else {
                        try {
                            playerBukkit.playSound(p.getLocation(), Sound.valueOf("ENTITY_GHAST_SCREAM"), 1.0f, 1.1f);
                        } catch (IllegalArgumentException e) {
                            playerBukkit.playSound(p.getLocation(), Sound.valueOf("GHAST_SCREAM2"), 1.0f, 1.1f);
                        }
                    }
                });
            }, 1L);
        }
    }
}