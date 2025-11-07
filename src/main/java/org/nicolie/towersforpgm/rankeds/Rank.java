package org.nicolie.towersforpgm.rankeds;

import org.nicolie.towersforpgm.utils.LanguageManager;

public enum Rank {
  BRONZE(LanguageManager.langMessage("ranked.ranks.bronze"), "§7", -100, -1, 35, -20),
  BRONZE_PLUS(LanguageManager.langMessage("ranked.ranks.bronze") + "+", "§7", 0, 99, 25, -20),
  SILVER(LanguageManager.langMessage("ranked.ranks.silver"), "§8", 100, 199, 25, -20),
  SILVER_PLUS(LanguageManager.langMessage("ranked.ranks.silver") + "+", "§8", 200, 299, 25, -20),
  GOLD(LanguageManager.langMessage("ranked.ranks.gold"), "§6", 300, 399, 25, -25),
  GOLD_PLUS(LanguageManager.langMessage("ranked.ranks.gold") + "+", "§6", 400, 499, 25, -25),
  EMERALD(LanguageManager.langMessage("ranked.ranks.emerald"), "§2", 500, 599, 20, -25),
  EMERALD_PLUS(LanguageManager.langMessage("ranked.ranks.emerald") + "+", "§2", 600, 699, 20, -25),
  DIAMOND(
      LanguageManager.langMessage("ranked.ranks.diamond"), "§9", 700, Integer.MAX_VALUE, 15, -25);

  private final String name;
  private final String color;
  private final int minElo;
  private final int maxElo;
  private final int eloWin;
  private final int eloLose;

  Rank(String name, String color, int minElo, int maxElo, int eloWin, int eloLose) {
    this.name = name;
    this.color = color;
    this.minElo = minElo;
    this.maxElo = maxElo;
    this.eloWin = eloWin;
    this.eloLose = eloLose;
  }

  @Override
  public String toString() {
    return name;
  }

  public String getPrefixedRank(boolean withColor) {
    if (withColor) {
      return "§8[" + color + name + "§8]§r";
    } else {
      return "[" + name + "]";
    }
  }

  public String getColor() {
    return color;
  }

  public int getMinElo() {
    return minElo;
  }

  public int getMaxElo() {
    return maxElo;
  }

  public static Rank getRankByElo(int elo) {
    for (Rank rank : values()) {
      if (elo >= rank.getMinElo() && elo <= rank.getMaxElo()) {
        return rank;
      }
    }
    return BRONZE;
  }

  public int getEloWin() {
    return eloWin;
  }

  public int getEloLose() {
    return eloLose;
  }
}
