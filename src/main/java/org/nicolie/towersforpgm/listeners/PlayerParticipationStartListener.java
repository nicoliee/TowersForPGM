package org.nicolie.towersforpgm.listeners;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.Teams;
import org.nicolie.towersforpgm.utils.ConfigManager;

import net.kyori.adventure.text.Component;
import tc.oc.pgm.events.PlayerParticipationStartEvent;
import tc.oc.pgm.join.JoinRequest;

public class PlayerParticipationStartListener implements Listener {
    private final TowersForPGM plugin;
    private final Teams teams;
    private final Captains captains;
    private final Draft draft;
    public PlayerParticipationStartListener(Teams teams, Captains captains, Draft draft) {
        this.draft = draft;
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
        String mapName = event.getMatch().getMap().getName();
        boolean isPrivate = ConfigManager.isPrivateMatch(mapName);
        boolean hasCaptains = captains.isMatchWithCaptains();

        
        if (!isPrivate && !hasCaptains) {
            return; // Si no es privada ni con capitanes, permitir sin más
        }

        String playerName = event.getPlayer().getBukkit().getName();
        UUID playerUUID = event.getPlayer().getBukkit().getUniqueId();
        
        if (request.isForcedOr(JoinRequest.Flag.FORCE)) {
            return; // Permitir si el request es FORZADO
        }

        boolean isInAnyTeam = teams.isPlayerInAnyTeam(playerName);
        boolean isCaptain = captains.isCaptain(playerUUID);
        System.out.println("--------------------");
        System.out.println("PlayerParticipationStartListener: " + playerName);
        System.out.println(String.format("isInAnyTeam: %b, isCaptain: %b, isInTeam1: %b, isInTeam2: %b, isCaptain1: %b, isCaptain2: %b", 
            isInAnyTeam, isCaptain, teams.isPlayerInTeam(playerName, 1), teams.isPlayerInTeam(playerName, 2), captains.isCaptain1(playerUUID), captains.isCaptain2(playerUUID)));

        if (!isInAnyTeam && !isCaptain) {
            event.cancel(Component.text(plugin.getPluginMessage("join.notAllowed")));
            event.setCancelled(true);
            return;
        }
        
        // Si llega aquí, significa que está en un equipo o es capitán
        if (teams.isPlayerInTeam(playerName, 1) || captains.isCaptain1(playerUUID)) {
            event.cancel(Component.text(plugin.getPluginMessage("join.redTeam")));
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                draft.assignPlayerToTeam(event.getPlayer().getBukkit(), 1);
            }, 5L); // Esperar 5 ticks antes de asignar el equipo
            return;
        }
        
        if (teams.isPlayerInTeam(playerName, 2) || captains.isCaptain2(playerUUID)) {
            event.cancel(Component.text(plugin.getPluginMessage("join.blueTeam"))); // o el mensaje que corresponda
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                draft.assignPlayerToTeam(event.getPlayer().getBukkit(), 2);
            }, 5L); // Esperar 5 ticks antes de asignar el equipo
            return;
        }        
    }
}