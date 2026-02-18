package org.nicolie.towersforpgm.rankeds.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.events.PlayerParticipationStopEvent;
import tc.oc.pgm.join.JoinRequest;

public class RankedPlayerParticipationListener implements Listener {

  @EventHandler
  public void onPlayerLeave(PlayerParticipationStopEvent event) {
    JoinRequest request = null;

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
