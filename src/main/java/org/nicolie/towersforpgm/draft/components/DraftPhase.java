package org.nicolie.towersforpgm.draft.components;

public enum DraftPhase {
  /** Draft is not active. No draft is currently in progress. */
  IDLE,

  /**
   * Initial phase where players can request a reroll of the captains. Players vote to decide
   * whether to reroll the captain selection.
   */
  CAPTAINS,

  /**
   * Reroll phase where players vote to select new captains from multiple options. This phase is
   * only active if reroll was approved in the CAPTAINS phase.
   */
  REROLL,

  /**
   * Active picking phase where captains alternate selecting players for their teams. This is the
   * main draft phase where team composition is determined.
   */
  RUNNING,

  /** Draft has concluded. Teams are finalized and ready to start the match. */
  ENDED;

  public boolean isActive() {
    return this != IDLE && this != ENDED;
  }

  public boolean requiresPlayerInput() {
    return this == CAPTAINS || this == REROLL || this == RUNNING;
  }

  public boolean isVotingPhase() {
    return this == CAPTAINS || this == REROLL;
  }
}
