package org.nicolie.towersforpgm.draft.state;

public enum AssignmentStrategy {
  DRAFT,
  MATCHMAKING;

  public boolean hasPicks() {
    return this == DRAFT;
  }
}
