package org.nicolie.towersforpgm.rankeds;

import static net.kyori.adventure.text.Component.newline;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.util.text.TextFormatter;

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
  private final String mapVote;
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
      String mapVote,
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
    this.mapVote = mapVote;
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

  public String getMapVote() {
    return mapVote;
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

  public Component getFormattedInfo() {
    Component header = Component.translatable("ranked.config.profile.header", Component.text(name));

    Component players = Component.translatable(
        "ranked.config.profile.players",
        Component.text(String.valueOf(min)),
        Component.text(String.valueOf(max)));

    Component matchmakingMode = matchmaking
        ? Component.translatable("ranked.config.profile.matchmakingAutomatic")
        : Component.translatable("ranked.config.profile.matchmakingCaptains");
    Component matchmakingLine =
        Component.translatable("ranked.config.profile.matchmaking", matchmakingMode);

    Component infoComponent = Component.empty()
        .appendNewline()
        .appendNewline()
        .append(header)
        .appendNewline()
        .appendNewline()
        .append(players)
        .appendNewline()
        .append(matchmakingLine)
        .appendNewline();

    if (!matchmaking) {
      Component orderLine =
          Component.translatable("ranked.config.profile.order", Component.text(order));

      Component rerollText = reroll
          ? Component.translatable("ranked.config.profile.rerollYes")
          : Component.translatable("ranked.config.profile.rerollNo");
      Component rerollLine = Component.translatable("ranked.config.profile.reroll", rerollText);

      infoComponent =
          infoComponent.append(orderLine).appendNewline().append(rerollLine).appendNewline();
    }

    infoComponent = infoComponent
        .append(Component.translatable("ranked.config.profile.table", Component.text(table)))
        .appendNewline()
        .append(Component.translatable("ranked.config.profile.mapPool", Component.text(mapPool)))
        .appendNewline()
        .append(Component.translatable("ranked.config.profile.timers"))
        .appendNewline()
        .append(Component.translatable(
            "ranked.config.profile.timerWaiting", Component.text(String.valueOf(timerWaiting))))
        .appendNewline()
        .append(Component.translatable(
            "ranked.config.profile.timerMinReached",
            Component.text(String.valueOf(timerMinReached))))
        .appendNewline()
        .append(Component.translatable(
            "ranked.config.profile.timerFull", Component.text(String.valueOf(timerFull))))
        .appendNewline()
        .append(Component.translatable(
            "ranked.config.profile.timerDisconnect",
            Component.text(String.valueOf(timerDisconnect))))
        .appendNewline();

    return infoComponent;
  }

  public Component getFormattedInfoShort() {
    Component line = TextFormatter.horizontalLine(
        NamedTextColor.WHITE, TextFormatter.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH * 3);
    Component header = Component.translatable(
            "ranked.config.profile.header", Component.text(name).color(NamedTextColor.WHITE))
        .color(NamedTextColor.GRAY);
    Component table = Component.translatable(
            "ranked.config.profile.table", Component.text(getTable()).color(NamedTextColor.WHITE))
        .color(NamedTextColor.GRAY);
    Component players = Component.translatable(
            "ranked.config.profile.players",
            Component.text(String.valueOf(min)).color(NamedTextColor.WHITE),
            Component.text(String.valueOf(max)).color(NamedTextColor.WHITE))
        .color(NamedTextColor.GRAY);
    Component pool = Component.translatable(
            "ranked.config.profile.mapPool", Component.text(mapPool).color(NamedTextColor.WHITE))
        .color(NamedTextColor.GRAY);
    Component info = Component.empty()
        .append(line)
        .append(newline())
        .append(header)
        .append(newline())
        .append(Component.space())
        .append(newline())
        .append(table)
        .append(newline())
        .append(players)
        .append(newline())
        .append(pool)
        .append(newline())
        .append(line);

    return info;
  }
}
