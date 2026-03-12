package org.nicolie.towersforpgm.draft.events;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;

public class DraftPickPlayerEvent extends Event {
  private static final HandlerList handlers = new HandlerList();
  private final UUID captain1;
  private final UUID captain2;
  private final String pickedPlayer;
  private final boolean isCaptain1Pick;
  private final boolean isAutomaticPick;
  private final Match match;

  public DraftPickPlayerEvent(
      UUID captain1,
      UUID captain2,
      String pickedPlayer,
      boolean isCaptain1Pick,
      boolean isAutomaticPick,
      Match match) {
    this.captain1 = captain1;
    this.captain2 = captain2;
    this.pickedPlayer = pickedPlayer;
    this.isCaptain1Pick = isCaptain1Pick;
    this.isAutomaticPick = isAutomaticPick;
    this.match = match;
  }

  public UUID getCaptain1() {
    return captain1;
  }

  public UUID getCaptain2() {
    return captain2;
  }

  public String getPickedPlayer() {
    return pickedPlayer;
  }

  public boolean isCaptain1Pick() {
    return isCaptain1Pick;
  }

  public boolean isAutomaticPick() {
    return isAutomaticPick;
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
