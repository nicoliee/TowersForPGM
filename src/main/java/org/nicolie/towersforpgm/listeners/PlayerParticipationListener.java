package org.nicolie.towersforpgm.listeners;

import java.util.UUID;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Teams;
import org.nicolie.towersforpgm.utils.LanguageManager;

import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.events.PlayerParticipationStartEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.join.JoinRequest;

// El plugin espera que los nombres de los teams sean "red" y "blue", por lo que no se pueden cambiar dinámicamente
// en el futuro se podría agregar un comando para cambiar los nombres de los teams, pero por ahora no es necesario.

public class PlayerParticipationListener implements Listener {
    private final Teams teams;
    private final Captains captains;
    private final LanguageManager languageManager;
    private final TowersForPGM plugin = TowersForPGM.getInstance();

    public PlayerParticipationListener(Teams teams, Captains captains, LanguageManager languageManager) {
        this.teams = teams;
        this.captains = captains;
        this.languageManager = languageManager;
    }

    @EventHandler
    public void onTeamChange(PlayerPartyChangeEvent event) {
        if (!captains.isMatchWithCaptains()) return;
        boolean isMatchFinished = event.getMatch().getPhase() == MatchPhase.FINISHED;
        if (isMatchFinished) return;

        JoinRequest request = event.getRequest() instanceof JoinRequest ? (JoinRequest) event.getRequest() : null;
        if (request == null || !request.isForcedOr(JoinRequest.Flag.FORCE)) return;

        String playerName = event.getPlayer().getBukkit().getName();

        if (event.getNewParty() == null) {
            return;
        }

        String newParty = event.getNewParty().getDefaultName().toLowerCase();
        if (newParty.equalsIgnoreCase("red")) {
            teams.forceTeam(event.getPlayer(), 1);
        } else if (newParty.equalsIgnoreCase("blue")) {
            teams.forceTeam(event.getPlayer(), 2);
        } else if (newParty.equalsIgnoreCase("observers") && event.getOldParty() != null) {
            teams.removeFromAnyTeam(playerName);
        }
    }

    @EventHandler
    public void onParticipate(PlayerParticipationStartEvent event) {
        if (!captains.isMatchWithCaptains()) return;
        JoinRequest request = null;

        // Verifica si la solicitud es de tipo JoinRequest
        if (event.getRequest() instanceof JoinRequest) {
            request = (JoinRequest) event.getRequest();
        }

        if (request.isForcedOr(JoinRequest.Flag.FORCE)) {
            return;
        }

        String playerName = event.getPlayer().getBukkit().getName();
        UUID playerUUID = event.getPlayer().getBukkit().getUniqueId();
        boolean isInAnyTeam = teams.isPlayerInAnyTeam(playerName);
        boolean isCaptain = captains.isCaptain(playerUUID);

        if(teams.isPlayerInTeam(playerName, 1) || captains.isCaptain1(playerUUID)){
            event.cancel(Component.text(languageManager.getPluginMessage("join.redTeam")));
            new BukkitRunnable() {
                @Override
                public void run() {
                    teams.assignTeam(event.getPlayer().getBukkit(), 1);
                }
            }.runTaskLater(plugin, 1);
            return;
        } else if(teams.isPlayerInTeam(playerName, 2) || captains.isCaptain2(playerUUID)){
            event.cancel(Component.text(languageManager.getPluginMessage("join.blueTeam")));
            new BukkitRunnable() {
                @Override
                public void run() {
                    teams.assignTeam(event.getPlayer().getBukkit(), 2);
                }
            }.runTaskLater(plugin, 1);
            return;
        }

        if (!isInAnyTeam && !isCaptain) {
            event.cancel(Component.text(languageManager.getPluginMessage("join.notAllowed")));
            return;
        }
    }
}