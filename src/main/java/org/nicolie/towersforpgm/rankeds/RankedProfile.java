package org.nicolie.towersforpgm.rankeds;

import org.nicolie.towersforpgm.utils.LanguageManager;

public class RankedProfile {
  private final String name;
  private final int min;
  private final int max;
  private final int timerWaiting;
  private final int timerMinReached;
  private final int timerFull;
  private final int timerDisconnect;
  private final boolean matchmaking;
  private final String order;
  private final boolean reroll;
  private final String table;
  private String mapPool;

  public RankedProfile(
      String name,
      int min,
      int max,
      int timerWaiting,
      int timerMinReached,
      int timerFull,
      int timerDisconnect,
      boolean matchmaking,
      String order,
      boolean reroll,
      String table,
      String mapPool) {
    this.name = name;
    this.min = min;
    this.max = max;
    this.timerWaiting = timerWaiting;
    this.timerMinReached = timerMinReached;
    this.timerFull = timerFull;
    this.timerDisconnect = timerDisconnect;
    this.matchmaking = matchmaking;
    this.order = order;
    this.reroll = reroll;
    this.table = table;
    this.mapPool = mapPool;
  }

  public String getName() {
    return name;
  }

  public int getMin() {
    return min;
  }

  public int getMax() {
    return max;
  }

  public int getTimerWaiting() {
    return timerWaiting;
  }

  public int getTimerMinReached() {
    return timerMinReached;
  }

  public int getTimerFull() {
    return timerFull;
  }

  public int getTimerDisconnect() {
    return timerDisconnect;
  }

  public boolean isMatchmaking() {
    return matchmaking;
  }

  public String getOrder() {
    return order;
  }

  public boolean isReroll() {
    return reroll;
  }

  public String getTable() {
    return table;
  }

  public String getMapPool() {
    return mapPool;
  }

  public void setMapPool(String mapPool) {
    this.mapPool = mapPool;
  }

  public String getFormattedInfo() {
    StringBuilder info = new StringBuilder();

    info.append(LanguageManager.message("ranked.config.profile.separator")).append("\n");
    info.append(LanguageManager.message("ranked.config.profile.header").replace("{name}", name))
        .append("\n");
    info.append(LanguageManager.message("ranked.config.profile.separator")).append("\n");
    info.append(LanguageManager.message("ranked.config.profile.players")
            .replace("{min}", String.valueOf(min))
            .replace("{max}", String.valueOf(max)))
        .append("\n");

    String matchmakingMode = matchmaking
        ? LanguageManager.message("ranked.config.profile.matchmakingAutomatic")
        : LanguageManager.message("ranked.config.profile.matchmakingCaptains");
    info.append(LanguageManager.message("ranked.config.profile.matchmaking")
            .replace("{mode}", matchmakingMode))
        .append("\n");

    if (!matchmaking) {
      info.append(LanguageManager.message("ranked.config.profile.order").replace("{order}", order))
          .append("\n");
      String rerollText = reroll
          ? LanguageManager.message("ranked.config.profile.rerollYes")
          : LanguageManager.message("ranked.config.profile.rerollNo");
      info.append(LanguageManager.message("ranked.config.profile.reroll")
              .replace("{reroll}", rerollText))
          .append("\n");
    }

    info.append(LanguageManager.message("ranked.config.profile.table").replace("{table}", table))
        .append("\n");
    info.append(LanguageManager.message("ranked.config.profile.mapPool").replace("{pool}", mapPool))
        .append("\n");
    info.append(LanguageManager.message("ranked.config.profile.timers")).append("\n");
    info.append(LanguageManager.message("ranked.config.profile.timerWaiting")
            .replace("{time}", String.valueOf(timerWaiting)))
        .append("\n");
    info.append(LanguageManager.message("ranked.config.profile.timerMinReached")
            .replace("{time}", String.valueOf(timerMinReached)))
        .append("\n");
    info.append(LanguageManager.message("ranked.config.profile.timerFull")
            .replace("{time}", String.valueOf(timerFull)))
        .append("\n");
    info.append(LanguageManager.message("ranked.config.profile.timerDisconnect")
            .replace("{time}", String.valueOf(timerDisconnect)))
        .append("\n");
    info.append(LanguageManager.message("ranked.config.profile.separator"));
    return info.toString();
  }
}
