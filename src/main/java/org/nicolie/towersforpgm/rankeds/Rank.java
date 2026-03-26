package org.nicolie.towersforpgm.rankeds;

import java.awt.Color;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.utils.LanguageManager;

public enum Rank {
  BRONZE(
      "ranked.ranks.bronze",
      RankVariant.BASE,
      -100,
      -1,
      35,
      -20,
      NamedTextColor.DARK_GRAY,
      new Color(150, 75, 0)),
  BRONZE_PLUS(
      "ranked.ranks.bronze",
      RankVariant.PLUS,
      0,
      99,
      25,
      -20,
      NamedTextColor.DARK_GRAY,
      new Color(150, 75, 0)),

  SILVER(
      "ranked.ranks.silver",
      RankVariant.BASE,
      100,
      199,
      25,
      -20,
      NamedTextColor.GRAY,
      Color.LIGHT_GRAY),
  SILVER_PLUS(
      "ranked.ranks.silver",
      RankVariant.PLUS,
      200,
      299,
      25,
      -20,
      NamedTextColor.GRAY,
      Color.LIGHT_GRAY),

  GOLD(
      "ranked.ranks.gold",
      RankVariant.BASE,
      300,
      399,
      25,
      -25,
      NamedTextColor.GOLD,
      new Color(255, 215, 0)),
  GOLD_PLUS(
      "ranked.ranks.gold",
      RankVariant.PLUS,
      400,
      499,
      25,
      -25,
      NamedTextColor.GOLD,
      new Color(255, 215, 0)),

  EMERALD(
      "ranked.ranks.emerald",
      RankVariant.BASE,
      500,
      599,
      20,
      -25,
      NamedTextColor.GREEN,
      new Color(0, 153, 51)),
  EMERALD_PLUS(
      "ranked.ranks.emerald",
      RankVariant.PLUS,
      600,
      699,
      20,
      -25,
      NamedTextColor.GREEN,
      new Color(0, 153, 51)),

  DIAMOND(
      "ranked.ranks.diamond",
      RankVariant.BASE,
      700,
      Integer.MAX_VALUE,
      15,
      -25,
      NamedTextColor.AQUA,
      new Color(135, 206, 235));

  private final String key;
  private final RankVariant variant;
  private final int minElo;
  private final int maxElo;
  private final int eloWin;
  private final int eloLose;
  private final NamedTextColor textColor;
  private final Color embedColor;

  Rank(
      String key,
      RankVariant variant,
      int minElo,
      int maxElo,
      int eloWin,
      int eloLose,
      NamedTextColor textColor,
      Color embedColor) {
    this.key = key;
    this.variant = variant;
    this.minElo = minElo;
    this.maxElo = maxElo;
    this.eloWin = eloWin;
    this.eloLose = eloLose;
    this.textColor = textColor;
    this.embedColor = embedColor;
  }

  public boolean isVariant(RankVariant variant) {
    return this.variant == variant;
  }

  public String getPrefixedRank(boolean withColor) {
    String name = getName();

    if (withColor) {
      return "§8[" + textColor.toString() + name + "§8]§r";
    } else {
      return "[" + name + "]";
    }
  }

  public String getName() {
    String base = LanguageManager.message(key);
    return base + variant.getDisplaySuffix();
  }

  public Component getNameComponent(boolean withBrackets) {
    Component baseName = Component.empty()
        .append(Component.translatable(key))
        .append(Component.text(variant.getDisplaySuffix()));

    Component name = baseName.color(textColor);

    if (withBrackets) {
      name = Component.text("[")
          .color(NamedTextColor.DARK_GRAY)
          .append(name)
          .append(Component.text("]").color(NamedTextColor.DARK_GRAY));
    }

    return name;
  }

  public NamedTextColor getColor() {
    return textColor;
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
    if (!MatchBotConfig.isVoiceChatEnabled()) return null;

    // Por el momento no hay roles para los rangos plus
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
