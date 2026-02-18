package org.nicolie.towersforpgm.draft.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Teams;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.teams.Team;

public class PlayerPartyChangeListener implements Listener {
  private final Teams teams;
  private final Captains captains;

  public PlayerPartyChangeListener(Teams teams, Captains captains) {
    this.teams = teams;
    this.captains = captains;
  }

  @EventHandler
  public void onPlayerPartyChange(PlayerPartyChangeEvent event) {
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
}
