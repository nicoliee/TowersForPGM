package org.nicolie.towersforpgm.database.models;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class Stats {
  private String username;
  private int kills;
  private int maxKills;
  private int deaths;
  private int assists;
  private double damageDone;
  private double damageTaken;
  private int points;
  private int maxPoints;
  private int wins;
  private int games;
  private int winstreak;
  private int maxWinstreak;
  private int elo;
  private int lastElo;
  private int maxElo;
  private int eloChange;

  public Stats(
      String username,
      int kills,
      int maxKills,
      int deaths,
      int assists,
      double damageDone,
      double damageTaken,
      int points,
      int maxPoints,
      int wins,
      int games,
      int winstreak,
      int maxWinstreak,
      int elo,
      int lastElo,
      int maxElo) {
    this.username = username;
    this.kills = kills;
    this.maxKills = maxKills;
    this.deaths = deaths;
    this.assists = assists;
    this.damageDone = damageDone;
    this.damageTaken = damageTaken;
    this.points = points;
    this.maxPoints = maxPoints;
    this.wins = wins;
    this.games = games;
    this.winstreak = winstreak;
    this.maxWinstreak = maxWinstreak;
    this.elo = elo;
    this.lastElo = lastElo;
    this.maxElo = maxElo;
    this.eloChange = elo - lastElo;
  }

  @Override
  public String toString() {
    return "Stats{" + "username='"
        + username + '\'' + ", kills="
        + kills + ", maxKills="
        + maxKills + ", deaths="
        + deaths + ", assists="
        + assists + ", damageDone="
        + damageDone + ", damageTaken="
        + damageTaken + ", points="
        + points + ", maxPoints="
        + maxPoints + ", wins="
        + wins + ", games="
        + games + ", winstreak="
        + winstreak + ", maxWinstreak="
        + maxWinstreak + ", elo="
        + elo + ", lastElo="
        + lastElo + ", maxElo="
        + maxElo + ", eloChange="
        + eloChange + '}';
  }

  // Getters
  public String getUsername() {
    return username;
  }

  public int getKills() {
    return kills;
  }

  public int getMaxKills() {
    return maxKills;
  }

  public int getDeaths() {
    return deaths;
  }

  public int getAssists() {
    return assists;
  }

  public double getDamageDone() {
    return damageDone;
  }

  public double getDamageTaken() {
    return damageTaken;
  }

  public int getPoints() {
    return points;
  }

  public int getMaxPoints() {
    return maxPoints;
  }

  public int getWins() {
    return wins;
  }

  public int getGames() {
    return games;
  }

  public int getWinstreak() {
    return winstreak;
  }

  public int getMaxWinstreak() {
    return maxWinstreak;
  }

  public int getElo() {
    return elo;
  }

  public int getLastElo() {
    return lastElo;
  }

  public int getMaxElo() {
    return maxElo;
  }

  public int getEloChange() {
    return eloChange;
  }

  // Métodos calculados adicionales
  public double getKDRatio() {
    if (deaths == 0) {
      return kills > 0 ? kills : 0.0;
    } else {
      return (double) kills / deaths;
    }
  }

  public List<Component> getLore() {
    List<Component> lore = Lists.newArrayList();
    Component eloComponent = Component.text(String.valueOf(elo))
        .append(Component.space())
        .append(Component.text("[", NamedTextColor.DARK_GRAY))
        .append(Component.text(String.valueOf(maxElo), NamedTextColor.DARK_GRAY))
        .append(Component.text("]", NamedTextColor.DARK_GRAY));

    if (elo != -9999) {
      lore.add(Component.translatable(
          "match.stats.type.generic", Component.translatable("misc.elo"), eloComponent));
      lore.add(Component.space());
    }

    Component combatLine = Component.translatable(
            "match.stats.type.kills", Component.text(kills).color(NamedTextColor.GREEN))
        .color(NamedTextColor.GRAY)
        .append(Component.space())
        .append(Component.translatable(
                "match.stats.type.deaths", Component.text(deaths).color(NamedTextColor.RED))
            .color(NamedTextColor.GRAY))
        .append(Component.space())
        .append(Component.translatable(
                "match.stats.type.kill_death_ratio",
                Component.text(String.format("%.2f", getKDRatio())).color(NamedTextColor.GREEN))
            .color(NamedTextColor.GRAY));
    int damageDonePerGame = games > 0 ? (int) (damageDone / games) : 0;
    int damageTakenPerGame = games > 0 ? (int) (damageTaken / games) : 0;
    lore.add(combatLine);
    lore.add(Component.translatable(
            "match.stats.type.generic",
            Component.translatable("stats.games"),
            Component.text(games).color(NamedTextColor.GREEN))
        .color(NamedTextColor.GRAY)
        .append(Component.space())
        .append(Component.translatable(
                "match.stats.type.generic",
                Component.translatable("stats.wins"),
                Component.text(wins).color(NamedTextColor.GREEN))
            .color(NamedTextColor.GRAY))
        .append(Component.space())
        .append(Component.translatable(
                "match.stats.type.generic",
                Component.translatable("stats.winstreak"),
                Component.text(winstreak).color(NamedTextColor.GREEN))
            .color(NamedTextColor.GRAY)));
    lore.add(Component.translatable(
            "match.stats.damage.dealt.melee",
            Component.text(damageDonePerGame + "❤").color(NamedTextColor.GREEN))
        .color(NamedTextColor.GRAY)
        .append(Component.space())
        .append(Component.translatable(
                "match.stats.damage.received.melee",
                Component.text(damageTakenPerGame + "❤").color(NamedTextColor.RED))
            .color(NamedTextColor.GRAY)));
    lore.add(Component.translatable(
            "match.stats.type.generic",
            Component.translatable("stats.points"),
            Component.text(points).color(NamedTextColor.GOLD))
        .color(NamedTextColor.GRAY));
    return lore;
  }
}
