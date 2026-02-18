package org.nicolie.towersforpgm.draft.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nicolie.towersforpgm.draft.core.Draft;
import tc.oc.pgm.api.match.event.MatchFinishEvent;

public class DraftMatchFinishListener implements Listener {
  private final Draft draft;

  public DraftMatchFinishListener(Draft draft) {
    this.draft = draft;
  }

  @EventHandler
  public void onMatchFinish(MatchFinishEvent event) {
    draft.cleanLists();
  }
}
