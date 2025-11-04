package org.nicolie.towersforpgm.draft.events;

import java.util.List;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;

public class MatchmakingEndEvent extends Event {
  private static final HandlerList handlers = new HandlerList();

  private final List<String> team1;
  private final List<String> team2;
  private final Match match;

  public MatchmakingEndEvent(List<String> team1, List<String> team2, Match match) {
    this.team1 = team1;
    this.team2 = team2;
    this.match = match;
  }

  public List<String> getTeam1() {
    return team1;
  }

  public List<String> getTeam2() {
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
