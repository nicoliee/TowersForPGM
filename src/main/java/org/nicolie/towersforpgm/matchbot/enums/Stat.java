package org.nicolie.towersforpgm.matchbot.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Stat {
  KILLS("Kills", "kills", false, true, true),
  MAX_KILLS("Max Kills", "maxKills", false, true, false),
  DEATHS("Deaths", "deaths", false, true, true),
  ASSISTS("Assists", "assists", false, true, true),
  DAMAGE_DONE("Damage Done", "damageDone", false, false, false),
  DAMAGE_TAKEN("Damage Taken", "damageTaken", false, false, false),
  POINTS("Points", "points", false, true, true),
  MAX_POINTS("Max Points", "maxPoints", false, true, false),
  WINS("Wins", "wins", false, true, false),
  GAMES("Games", "games", false, true, false),
  WINSTREAK("Winstreak", "winstreak", false, true, false),
  MAX_WINSTREAK("Max Winstreak", "maxWinstreak", false, true, false),
  WINRATE("Winrate", "winrate", true, false, false),
  WL("W/L", "wlRatio", false, false, false),
  KD("K/D", "kdRatio", false, false, false),
  ELO("ELO", "elo", false, true, false),
  MAX_ELO("Max ELO", "maxElo", false, true, false);
  private final String displayName;
  private final String dbColumn;
  private final boolean isPercentage;
  private final boolean isInteger;
  private final boolean statPerGame;

  Stat(
      String displayName,
      String dbColumn,
      boolean isPercentage,
      boolean isInteger,
      boolean statPerGame) {
    this.displayName = displayName;
    this.dbColumn = dbColumn;
    this.isPercentage = isPercentage;
    this.isInteger = isInteger;
    this.statPerGame = statPerGame;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDbColumn() {
    return dbColumn;
  }

  public boolean isPercentage() {
    return isPercentage;
  }

  public boolean isInteger() {
    return isInteger;
  }

  public boolean isStatPerGame() {
    return statPerGame;
  }

  public static List<String> getAllStatNames() {
    return Arrays.stream(Stat.values()).map(Stat::getDisplayName).collect(Collectors.toList());
  }

  public static Stat fromDisplayName(String name) {
    for (Stat stat : Stat.values()) {
      if (stat.getDisplayName().equalsIgnoreCase(name)) {
        return stat;
      }
    }
    return null;
  }
}
