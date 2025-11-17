package org.nicolie.towersforpgm.rankeds;

import java.awt.Color;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.utils.LanguageManager;

public enum Rank {
  BRONZE(
      LanguageManager.langMessage("ranked.ranks.bronze"),
      "§7",
      -100,
      -1,
      35,
      -20,
      new Color(150, 75, 0)),
  BRONZE_PLUS(
      LanguageManager.langMessage("ranked.ranks.bronze") + "+",
      "§7",
      0,
      99,
      25,
      -20,
      new Color(150, 75, 0)),
  SILVER(
      LanguageManager.langMessage("ranked.ranks.silver"),
      "§8",
      100,
      199,
      25,
      -20,
      Color.LIGHT_GRAY),
  SILVER_PLUS(
      LanguageManager.langMessage("ranked.ranks.silver") + "+",
      "§8",
      200,
      299,
      25,
      -20,
      Color.LIGHT_GRAY),
  GOLD(
      LanguageManager.langMessage("ranked.ranks.gold"),
      "§6",
      300,
      399,
      25,
      -25,
      new Color(255, 215, 0)),
  GOLD_PLUS(
      LanguageManager.langMessage("ranked.ranks.gold") + "+",
      "§6",
      400,
      499,
      25,
      -25,
      new Color(255, 215, 0)),
  EMERALD(
      LanguageManager.langMessage("ranked.ranks.emerald"),
      "§2",
      500,
      599,
      20,
      -25,
      new Color(0, 153, 51)),
  EMERALD_PLUS(
      LanguageManager.langMessage("ranked.ranks.emerald") + "+",
      "§2",
      600,
      699,
      20,
      -25,
      new Color(0, 153, 51)),
  DIAMOND(
      LanguageManager.langMessage("ranked.ranks.diamond"),
      "§9",
      700,
      Integer.MAX_VALUE,
      15,
      -25,
      new Color(135, 206, 235));
  private final String name;
  private final String color;
  private final int minElo;
  private final int maxElo;
  private final int eloWin;
  private final int eloLose;
  private final Color embedColor;

  Rank(
      String name,
      String color,
      int minElo,
      int maxElo,
      int eloWin,
      int eloLose,
      Color embedColor) {
    this.name = name;
    this.color = color;
    this.minElo = minElo;
    this.maxElo = maxElo;
    this.eloWin = eloWin;
    this.eloLose = eloLose;
    this.embedColor = embedColor;
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

  public Color getEmbedColor() {
    return embedColor;
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

  public String getRoleID() {
    if (!MatchBotConfig.isRankedEnabled()) return null;

    switch (this) {
      case BRONZE:
      case BRONZE_PLUS:
        return MatchBotConfig.getBronzeRoleId();
      case SILVER:
      case SILVER_PLUS:
        return MatchBotConfig.getSilverRoleId();
      case GOLD:
      case GOLD_PLUS:
        return MatchBotConfig.getGoldRoleId();
      case EMERALD:
      case EMERALD_PLUS:
        return MatchBotConfig.getEmeraldRoleId();
      case DIAMOND:
        return MatchBotConfig.getDiamondRoleId();
      default:
        return null;
    }
  }
}
