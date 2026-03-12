package org.nicolie.towersforpgm.draft.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.draft.components.DraftPhase;
import org.nicolie.towersforpgm.draft.core.Draft;
import tc.oc.pgm.events.PlayerParticipationStopEvent;
import tc.oc.pgm.join.JoinRequest;

public class DraftPlayerParticipationStopListener implements Listener {
  @EventHandler
  public void onPlayerLeave(PlayerParticipationStopEvent event) {
    JoinRequest request = null;

    if (event.getRequest() instanceof JoinRequest) {
      request = (JoinRequest) event.getRequest();
    }

    if (request.isForcedOr(JoinRequest.Flag.FORCE)) {
      return;
    }
    if (Draft.getPhase() == DraftPhase.RUNNING) {
      event.cancel(Component.translatable("draft.leave.notAllowed"));
      return;
    }
  }
}
