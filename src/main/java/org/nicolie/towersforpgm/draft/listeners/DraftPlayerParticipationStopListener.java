package org.nicolie.towersforpgm.draft.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.draft.state.DraftPhase;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import tc.oc.pgm.events.PlayerParticipationStopEvent;
import tc.oc.pgm.join.JoinRequest;

public class DraftPlayerParticipationStopListener implements Listener {

  @EventHandler
  public void onPlayerLeave(PlayerParticipationStopEvent event) {
    JoinRequest request =
        event.getRequest() instanceof JoinRequest ? (JoinRequest) event.getRequest() : null;

    if (request != null && request.isForcedOr(JoinRequest.Flag.FORCE)) return;

    var session = MatchSessionRegistry.get(event.getMatch());
    if (session == null) return;

    DraftContext ctx = session.getDraft();
    if (ctx != null && ctx.phase() == DraftPhase.RUNNING) {
      event.cancel(Component.translatable("draft.leave.notAllowed"));
    }
  }
}
