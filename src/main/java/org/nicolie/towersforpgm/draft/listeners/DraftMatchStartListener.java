package org.nicolie.towersforpgm.draft.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Utilities;
import tc.oc.pgm.api.match.event.MatchStartEvent;

/** Handles draft-related logic when a match starts. */
public class DraftMatchStartListener implements Listener {
  private final Captains captains;

  public DraftMatchStartListener(Captains captains) {
    this.captains = captains;
  }

  @EventHandler
  public void onMatchStart(MatchStartEvent event) {
    if (captains.isReadyActive()) {
      Utilities.cancelReadyReminder();
      captains.resetReady();
    }
  }
}
