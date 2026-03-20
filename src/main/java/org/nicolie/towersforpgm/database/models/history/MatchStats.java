package org.nicolie.towersforpgm.database.models.history;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import me.tbg.match.bot.stats.Stats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class MatchStats {
  private final String displayName;

  // K/D stats
  private final int kills;
  private final int deaths;
  private final int assists;
  private final int killstreak;
  private final int maxKillstreak;

  // Bow stats
  private final int longestBowKill;
  private final double bowDamage;
  private final double bowDamageTaken;
  private final int shotsTaken;
  private final int shotsHit;

  // Damage stats
  private final double damageDone;
  private final double damageTaken;

  // Objective stats
  private final int destroyablePiecesBroken;
  private final int monumentsDestroyed;
  private final int flagsCaptured;
  private final int flagPickups;
  private final int coresLeaked;
  private final int woolsCaptured;
  private final int woolsTouched;
  private final long longestFlagHoldMillis;
  private final int totalPoints;

  private final String teamName;

  public MatchStats(
      String displayName,
      int kills,
      int deaths,
      int assists,
      int killstreak,
      int maxKillstreak,
      int longestBowKill,
      double bowDamage,
      double bowDamageTaken,
      int shotsTaken,
      int shotsHit,
      double damageDone,
      double damageTaken,
      int destroyablePiecesBroken,
      int monumentsDestroyed,
      int flagsCaptured,
      int flagPickups,
      int coresLeaked,
      int woolsCaptured,
      int woolsTouched,
      long longestFlagHoldMillis,
      int totalPoints,
      String teamName) {
    this.displayName = displayName;
    this.kills = kills;
    this.deaths = deaths;
    this.assists = assists;
    this.killstreak = killstreak;
    this.maxKillstreak = maxKillstreak;
    this.longestBowKill = longestBowKill;
    this.bowDamage = bowDamage;
    this.bowDamageTaken = bowDamageTaken;
    this.shotsTaken = shotsTaken;
    this.shotsHit = shotsHit;
    this.damageDone = damageDone;
    this.damageTaken = damageTaken;
    this.destroyablePiecesBroken = destroyablePiecesBroken;
    this.monumentsDestroyed = monumentsDestroyed;
    this.flagsCaptured = flagsCaptured;
    this.flagPickups = flagPickups;
    this.coresLeaked = coresLeaked;
    this.woolsCaptured = woolsCaptured;
    this.woolsTouched = woolsTouched;
    this.longestFlagHoldMillis = longestFlagHoldMillis;
    this.totalPoints = totalPoints;
    this.teamName = teamName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public int getKills() {
    return kills;
  }

  public int getDeaths() {
    return deaths;
  }

  public int getAssists() {
    return assists;
  }

  public int getKillstreak() {
    return killstreak;
  }

  public int getMaxKillstreak() {
    return maxKillstreak;
  }

  public int getLongestBowKill() {
    return longestBowKill;
  }

  public double getBowDamage() {
    return bowDamage;
  }

  public double getBowDamageTaken() {
    return bowDamageTaken;
  }

  public int getShotsTaken() {
    return shotsTaken;
  }

  public int getShotsHit() {
    return shotsHit;
  }

  public double getDamageDone() {
    return damageDone;
  }

  public double getDamageTaken() {
    return damageTaken;
  }

  public int getDestroyablePiecesBroken() {
    return destroyablePiecesBroken;
  }

  public int getMonumentsDestroyed() {
    return monumentsDestroyed;
  }

  public int getFlagsCaptured() {
    return flagsCaptured;
  }

  public int getFlagPickups() {
    return flagPickups;
  }

  public int getCoresLeaked() {
    return coresLeaked;
  }

  public int getWoolsCaptured() {
    return woolsCaptured;
  }

  public int getWoolsTouched() {
    return woolsTouched;
  }

  public long getLongestFlagHoldMillis() {
    return longestFlagHoldMillis;
  }

  public int getTotalPoints() {
    return totalPoints;
  }

  public String getTeamName() {
    return teamName;
  }

  public double getKDRatio() {
    if (deaths == 0) {
      return kills > 0 ? kills : 0.0;
    }
    return (double) kills / deaths;
  }

  public double getAccuracy() {
    if (shotsTaken == 0) {
      return 0.0;
    }
    return (double) shotsHit / shotsTaken * 100.0;
  }

  public double getTotalDamageDone() {
    return damageDone + bowDamage;
  }

  public double getTotalDamageTaken() {
    return damageTaken + bowDamageTaken;
  }

  public Stats toStats() {
    return new Stats(
        displayName,
        kills,
        deaths,
        assists,
        killstreak,
        maxKillstreak,
        longestBowKill,
        bowDamage,
        bowDamageTaken,
        shotsTaken,
        shotsHit,
        damageDone,
        damageTaken,
        destroyablePiecesBroken,
        monumentsDestroyed,
        flagsCaptured,
        flagPickups,
        coresLeaked,
        woolsCaptured,
        woolsTouched,
        Duration.ofMillis(longestFlagHoldMillis),
        totalPoints,
        null // party
        );
  }

  public List<Component> getComponent() {
    List<Component> lore = new ArrayList<>();
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
    lore.add(combatLine);

    Component combatLine2 = Component.translatable(
            "match.stats.type.assists", Component.text(assists).color(NamedTextColor.GREEN))
        .color(NamedTextColor.GRAY)
        .append(Component.space())
        .append(Component.translatable(
                "match.stats.type.best_kill_streak",
                Component.text(maxKillstreak).color(NamedTextColor.GREEN))
            .color(NamedTextColor.GRAY));
    lore.add(combatLine2);

    Component damageDoneComponent = Component.translatable(
            "match.stats.damage.dealt",
            Component.text(String.format("%.1f", damageDone) + "❤").color(NamedTextColor.GREEN),
            Component.text(String.format("%.1f", bowDamage) + "❤").color(NamedTextColor.YELLOW))
        .color(NamedTextColor.GRAY);
    lore.add(damageDoneComponent);

    Component damageTakenComponent = Component.translatable(
            "match.stats.damage.received",
            Component.text(String.format("%.1f", damageTaken) + "❤").color(NamedTextColor.RED),
            Component.text(String.format("%.1f", bowDamageTaken) + "❤").color(NamedTextColor.GOLD))
        .color(NamedTextColor.GRAY);
    lore.add(damageTakenComponent);

    if (shotsTaken > 0) {
      double accuracy = (double) shotsHit / shotsTaken * 100;
      Component bowComponent = Component.translatable(
              "match.stats.bow",
              Component.text(shotsHit).color(NamedTextColor.YELLOW),
              Component.text(shotsTaken).color(NamedTextColor.YELLOW),
              Component.text(String.format("%.1f%%", accuracy)).color(NamedTextColor.YELLOW))
          .color(NamedTextColor.GRAY);
      lore.add(bowComponent);

      if (longestBowKill > 0) {
        Component longestBowKillComponent = Component.translatable(
                "match.stats.type.longest_bow_shot",
                Component.text(longestBowKill + "m").color(NamedTextColor.YELLOW))
            .color(NamedTextColor.GRAY);
        lore.add(longestBowKillComponent);
      }
    }

    boolean hasObjective = destroyablePiecesBroken > 0
        || monumentsDestroyed > 0
        || flagsCaptured > 0
        || flagPickups > 0
        || coresLeaked > 0
        || woolsCaptured > 0
        || woolsTouched > 0
        || longestFlagHoldMillis > 0;

    if (hasObjective) {
      lore.add(Component.empty());
      if (destroyablePiecesBroken > 0)
        lore.add(statLine("match.stats.broken.concise", destroyablePiecesBroken));
      if (monumentsDestroyed > 0)
        lore.add(statLine("match.stats.monuments.concise", monumentsDestroyed));
      if (coresLeaked > 0) lore.add(statLine("match.stats.coresLeaked.concise", coresLeaked));
      if (woolsCaptured > 0) lore.add(statLine("match.stats.woolsCaptured.concise", woolsCaptured));
      if (woolsTouched > 0) lore.add(statLine("match.stats.woolsTouched.concise", woolsTouched));
      if (flagsCaptured > 0) lore.add(statLine("match.stats.flagsCaptured.concise", flagsCaptured));
      if (flagPickups > 0) lore.add(statLine("match.stats.flagPickups.concise", flagPickups));
      if (longestFlagHoldMillis > 0) {
        long secs = longestFlagHoldMillis / 1000;
        lore.add(Component.translatable(
                "match.stats.flaghold.concise",
                Component.text(secs + "s").color(NamedTextColor.GRAY))
            .color(NamedTextColor.DARK_GRAY));
      }
    }

    lore.add(Component.empty());
    lore.add(Component.translatable(
            "match.stats.type.generic",
            Component.translatable("stats.points").color(NamedTextColor.DARK_GRAY),
            Component.text(totalPoints).color(NamedTextColor.GOLD))
        .color(NamedTextColor.DARK_GRAY));

    return lore;
  }

  private static Component statLine(String key, int value) {
    return Component.translatable(key, Component.text(value).color(NamedTextColor.GRAY))
        .color(NamedTextColor.DARK_GRAY);
  }
}
