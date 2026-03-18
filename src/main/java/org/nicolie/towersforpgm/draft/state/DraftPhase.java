package org.nicolie.towersforpgm.draft.state;

public enum DraftPhase {
  IDLE,
  CAPTAINS,
  REROLL,
  MAP,
  RUNNING,
  ENDED;

  public boolean isActive() {
    return this != IDLE && this != ENDED;
  }

  public boolean requiresPlayerInput() {
    return this == CAPTAINS || this == REROLL || this == MAP || this == RUNNING;
  }

  public boolean isVotingPhase() {
    return this == CAPTAINS || this == REROLL || this == MAP;
  }
}
