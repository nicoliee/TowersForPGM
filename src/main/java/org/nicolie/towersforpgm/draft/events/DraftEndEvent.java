package org.nicolie.towersforpgm.draft.events;

import java.util.Set;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;

public class DraftEndEvent extends Event {
  private static final HandlerList handlers = new HandlerList();

  private final Set<String> team1;
  private final Set<String> team2;
  private final Match match;

  public DraftEndEvent(Set<String> team1, Set<String> team2, Match match) {
    this.team1 = team1;
    this.team2 = team2;
    this.match = match;
  }

  public Set<String> getTeam1() {
    return team1;
  }

  public Set<String> getTeam2() {
    return team2;
  }

  public Match getMatch() {
    return match;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
