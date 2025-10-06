package org.nicolie.towersforpgm.rankeds;

import org.nicolie.towersforpgm.utils.LanguageManager;

public enum Rank {
  BRONZE(LanguageManager.langMessage("ranked.ranks.bronze"), "§7", -100, -1),
  BRONZE_PLUS(LanguageManager.langMessage("ranked.ranks.bronze") + "+", "§7", 0, 99),
  SILVER(LanguageManager.langMessage("ranked.ranks.silver"), "§8", 100, 199),
  SILVER_PLUS(LanguageManager.langMessage("ranked.ranks.silver") + "+", "§8", 200, 299),
  GOLD(LanguageManager.langMessage("ranked.ranks.gold"), "§6", 300, 399),
  GOLD_PLUS(LanguageManager.langMessage("ranked.ranks.gold") + "+", "§6", 400, 499),
  EMERALD(LanguageManager.langMessage("ranked.ranks.emerald"), "§2", 500, 599),
  EMERALD_PLUS(LanguageManager.langMessage("ranked.ranks.emerald") + "+", "§2", 600, 699),
  DIAMOND(LanguageManager.langMessage("ranked.ranks.diamond"), "§9", 700, Integer.MAX_VALUE);

  private final String name;
  private final String color;
  private final int minElo;
  private final int maxElo;

  Rank(String name, String color, int minElo, int maxElo) {
    this.name = name;
    this.color = color;
    this.minElo = minElo;
    this.maxElo = maxElo;
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
}
