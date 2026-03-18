package org.nicolie.towersforpgm.draft.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.draft.pick.gui.PicksGUIManager;
import org.nicolie.towersforpgm.draft.state.DraftPhase;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.join.JoinRequest;

public class PlayerPartyChangeListener implements Listener {

  @EventHandler
  public void onPlayerPartyChange(PlayerPartyChangeEvent event) {
    if (event.getMatch().isFinished()) return;

    var session = MatchSessionRegistry.get(event.getMatch());
    if (session == null) return;

    DraftContext ctx = session.getDraft();
    if (ctx == null) return;

    if (ctx.phase() == DraftPhase.RUNNING) {
      PicksGUIManager.giveItem(event.getPlayer().getBukkit());
    }

    if (!ctx.captains().isMatchWithCaptains()) return;

    // Solo nos interesa si el request es forzado
    JoinRequest request =
        event.getRequest() instanceof JoinRequest ? (JoinRequest) event.getRequest() : null;
    if (request == null || !request.isForcedOr(JoinRequest.Flag.FORCE)) return;

    // Desconexión — no hacer nada
    if (event.getNewParty() == null) return;

    String playerName = event.getPlayer().getBukkit().getName();
    Party newTeam = event.getNewParty();
    int teamNumber = ctx.teams().getTeamNumber(newTeam);

    if (teamNumber != -1) {
      ctx.teams().forceTeam(event.getPlayer(), teamNumber);
    } else if (event.getNewParty().isObserving() && event.getOldParty() != null) {
      ctx.teams().removeFromAnyTeam(playerName);
    }
  }
}
