package org.nicolie.towersforpgm.draft.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import tc.oc.pgm.api.match.event.MatchStartEvent;

public class DraftMatchStartListener implements Listener {

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchStart(MatchStartEvent event) {
    DraftContext ctx = getActiveDraft(event);
    if (ctx == null) return;

    if (ctx.captains().isReadyActive()) {
      ctx.readyReminder().cancelTimer();
      ctx.captains().resetReady();
    }
  }

  private DraftContext getActiveDraft(MatchStartEvent event) {
    var session = MatchSessionRegistry.get(event.getMatch());
    if (session == null) return null;
    return session.getDraft();
  }
}
