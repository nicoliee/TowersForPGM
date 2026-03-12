package org.nicolie.towersforpgm.draft.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.draft.components.DraftPhase;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Draft;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.draft.picksMenu.PicksGUIManager;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.join.JoinRequest;

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
    if (Draft.getPhase() == DraftPhase.RUNNING) {
      PicksGUIManager.giveItem(event.getPlayer().getBukkit());
    }
    if (captains.isMatchWithCaptains()) {
      // No nos interesa si el request no es forzado.
      JoinRequest request =
          event.getRequest() instanceof JoinRequest ? (JoinRequest) event.getRequest() : null;
      if (request == null || !request.isForcedOr(JoinRequest.Flag.FORCE)) return;

      String playerName = event.getPlayer().getBukkit().getName();

      // Pasa cuando una persona se desconecta, no hacer nada en este caso.
      if (event.getNewParty() == null) return;

      Party newTeam = event.getNewParty();
      int teamNumber = teams.getTeamNumber(newTeam);
      if (teamNumber != -1) {
        teams.forceTeam(event.getPlayer(), teamNumber);
      } else if (event.getNewParty().isObserving() && event.getOldParty() != null) {
        teams.removeFromAnyTeam(playerName);
      }
    }
  }
}
