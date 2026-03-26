package org.nicolie.towersforpgm.rankeds;

public enum RankVariant {
  BASE(""),
  PLUS(" +"),
  MINUS(" -");

  private final String displaySuffix;

  RankVariant(String displaySuffix) {
    this.displaySuffix = displaySuffix;
  }

  public String getDisplaySuffix() {
    return displaySuffix;
  }
}
