package org.nicolie.towersforpgm.draft.events;

import java.util.List;
import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class DraftStartEvent extends Event {
  private static final HandlerList handlers = new HandlerList();

  private final UUID captain1;
  private final UUID captain2;
  private final List<MatchPlayer> players;
  private final Match match;
  private final boolean randomizeOrder;

  public DraftStartEvent(
      UUID captain1,
      UUID captain2,
      List<MatchPlayer> players,
      Match match,
      boolean randomizeOrder) {
    this.captain1 = captain1;
    this.captain2 = captain2;
    this.players = players;
    this.match = match;
    this.randomizeOrder = randomizeOrder;
  }

  public UUID getCaptain1() {
    return captain1;
  }

  public UUID getCaptain2() {
    return captain2;
  }

  public List<MatchPlayer> getPlayers() {
    return players;
  }

  public Match getMatch() {
    return match;
  }

  public boolean isRandomizeOrder() {
    return randomizeOrder;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
