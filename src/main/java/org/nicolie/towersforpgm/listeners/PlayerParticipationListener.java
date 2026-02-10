package org.nicolie.towersforpgm.listeners;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.events.PlayerParticipationStartEvent;
import tc.oc.pgm.events.PlayerParticipationStopEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.teams.Team;

public class PlayerParticipationListener implements Listener {
  private final Teams teams;
  private final Captains captains;
  private final TowersForPGM plugin = TowersForPGM.getInstance();

  public PlayerParticipationListener(Teams teams, Captains captains) {
    this.teams = teams;
    this.captains = captains;
  }

  // Manejar el cambio de equipo con comando en teams
  @EventHandler
  public void onTeamChange(PlayerPartyChangeEvent event) {
    if (event.getMatch().isFinished()) return;
    if (captains.isMatchWithCaptains()) {
      JoinRequest request =
          event.getRequest() instanceof JoinRequest ? (JoinRequest) event.getRequest() : null;
      if (request == null || !request.isForcedOr(JoinRequest.Flag.FORCE)) return;

      String playerName = event.getPlayer().getBukkit().getName();

      // Pasa cuando una persona se desconecta, no hacer nada en este caso.
      if (event.getNewParty() == null) return;

      if (event.getNewParty() instanceof Team) {
        Team newTeam = (Team) event.getNewParty();
        int teamNumber = teams.getTeamNumber(newTeam);
        if (teamNumber != -1) {
          teams.forceTeam(event.getPlayer(), teamNumber);
        } else if (newTeam.isObserving() && event.getOldParty() != null) {
          teams.removeFromAnyTeam(playerName);
        }
      }
    }
  }

  // Meterlo a su team si hay draft
  @EventHandler
  public void onParticipate(PlayerParticipationStartEvent event) {
    if (captains.isMatchWithCaptains()) {
      JoinRequest request = null;
      if (event.getRequest() instanceof JoinRequest) {
        request = (JoinRequest) event.getRequest();
      }

      if (request.isForcedOr(JoinRequest.Flag.FORCE)) {
        return;
      }

      String playerName = event.getPlayer().getBukkit().getName();
      UUID playerUUID = event.getPlayer().getBukkit().getUniqueId();
      boolean isInAnyTeam = teams.isPlayerInAnyTeam(playerName);
      int captainNumber = captains.getCaptainTeam(playerUUID);

      int playerTeam = -1;
      if (teams.isPlayerInTeam(playerName, 1) || captainNumber == 1) {
        playerTeam = 1;
      } else if (teams.isPlayerInTeam(playerName, 2) || captainNumber == 2) {
        playerTeam = 2;
      }

      if (playerTeam != -1) {
        String teamName = teams.getTeamName(playerTeam);
        String message = LanguageManager.message("draft.join.team").replace("{team}", teamName);
        event.cancel(Component.text(message));

        final int finalTeam = playerTeam;
        new BukkitRunnable() {
          @Override
          public void run() {
            teams.assignTeam(event.getPlayer().getBukkit(), finalTeam);
          }
        }.runTaskLater(plugin, 1);
        return;
      }

      if (!isInAnyTeam && captainNumber == -1) {
        event.cancel(Component.text(LanguageManager.message("draft.join.notAllowed")));
        return;
      }
    }
  }

  // No dejar que un jugador se salga si es ranked
  @EventHandler
  public void onPlayerLeave(PlayerParticipationStopEvent event) {
    JoinRequest request = null;

    // Verifica si la solicitud es de tipo JoinRequest
    if (event.getRequest() instanceof JoinRequest) {
      request = (JoinRequest) event.getRequest();
    }

    if (request.isForcedOr(JoinRequest.Flag.FORCE)) {
      return;
    }
    if (Queue.isRanked() && event.getMatch().isRunning()) {
      event.cancel(Component.text(LanguageManager.message("ranked.notAllowed")));
      return;
    }
  }
}
