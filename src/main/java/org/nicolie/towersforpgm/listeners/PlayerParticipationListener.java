package org.nicolie.towersforpgm.listeners;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Teams;

import net.kyori.adventure.text.Component;
import tc.oc.pgm.events.PlayerParticipationStartEvent;
import tc.oc.pgm.events.PlayerParticipationStopEvent;
import tc.oc.pgm.join.JoinRequest;

public class PlayerParticipationListener implements Listener {
    private final TowersForPGM plugin;
    private final Teams teams;
    private final Captains captains;
    public PlayerParticipationListener(Teams teams, Captains captains) {
        this.plugin = TowersForPGM.getInstance();
        this.teams = teams;
        this.captains = captains;
    }
    @EventHandler
    public void onParticipate(PlayerParticipationStartEvent event) {
        JoinRequest request = null;
        
        // Verifica si la solicitud es de tipo JoinRequest
        if (event.getRequest() instanceof JoinRequest) {
            request = (JoinRequest) event.getRequest();
        }
        boolean hasCaptains = captains.isMatchWithCaptains();

        
        if (!hasCaptains) {
            return; // Si no es con capitanes, permitir sin más
        }

        String playerName = event.getPlayer().getBukkit().getName();
        UUID playerUUID = event.getPlayer().getBukkit().getUniqueId();
        
        if (request.isForcedOr(JoinRequest.Flag.FORCE)) {
            if(captains.isMatchWithCaptains()){
                if(request.getTeam().getDefaultName().equalsIgnoreCase("red")) {
                    teams.removeFromAnyTeam(playerName);
                    teams.addPlayerToTeam(playerName, 1);
                } else if(request.getTeam().getDefaultName().equalsIgnoreCase("blue")) {
                    teams.removeFromAnyTeam(playerName);
                    teams.addPlayerToTeam(playerName, 2);
                }
            }
            return;
        }

        boolean isInAnyTeam = teams.isPlayerInAnyTeam(playerName);
        boolean isCaptain = captains.isCaptain(playerUUID);
        if (!isInAnyTeam && !isCaptain) {
            event.cancel(Component.text(plugin.getPluginMessage("join.notAllowed")));
            event.setCancelled(true);
            return;
        }
        
        // Si llega aquí, significa que está en un equipo o es capitán
        if (teams.isPlayerInTeam(playerName, 1) || captains.isCaptain1(playerUUID)) {
            event.cancel(Component.text(plugin.getPluginMessage("join.redTeam")));
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                teams.assignTeam(event.getPlayer().getBukkit(), 1);
            }, 5L); // Esperar 5 ticks antes de asignar el equipo
            return;
        }
        
        if (teams.isPlayerInTeam(playerName, 2) || captains.isCaptain2(playerUUID)) {
            event.cancel(Component.text(plugin.getPluginMessage("join.blueTeam"))); // o el mensaje que corresponda
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                teams.assignTeam(event.getPlayer().getBukkit(), 2);
            }, 5L); // Esperar 5 ticks antes de asignar el equipo
            return;
        }        
    }

    @EventHandler
    public void onLeave(PlayerParticipationStopEvent event) {
        JoinRequest request = null;

        if (event.getRequest() instanceof JoinRequest) {
            request = (JoinRequest) event.getRequest();
        }
        if (request != null && request.isForcedOr(JoinRequest.Flag.FORCE)) {
            if (captains.isMatchWithCaptains()) {
                Player player = event.getPlayer().getBukkit();
                String playerName = player.getName();
                // Esperamos 5 ticks y verificamos si sigue online
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Player onlinePlayer = Bukkit.getPlayerExact(playerName);
                        if (onlinePlayer != null && onlinePlayer.isOnline()) {
                            teams.removeFromAnyTeam(playerName);
                        } else {
                        }
                    }
                }.runTaskLater(plugin, 5L);
            }
            return;
        }
    }
}