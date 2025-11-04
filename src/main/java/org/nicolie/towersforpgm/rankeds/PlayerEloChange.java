package org.nicolie.towersforpgm.rankeds;

import java.util.UUID;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.nicolie.towersforpgm.TowersForPGM;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public class PlayerEloChange {
  private final String username;
  private final int currentElo;
  private final int newElo;
  private final int maxElo;
  private final int eloChange;

  public PlayerEloChange(String username, int currentElo, int newElo, int eloChange, int maxElo) {
    this.username = username;
    this.currentElo = currentElo;
    this.newElo = newElo;
    this.eloChange = eloChange;
    this.maxElo = maxElo;
  }

  public String getUsername() {
    return username;
  }

  public int getCurrentElo() {
    return currentElo;
  }

  public int getNewElo() {
    return newElo;
  }

  public int getEloChange() {
    return eloChange;
  }

  public int getMaxElo() {
    return maxElo;
  }

  @Override
  public String toString() {
    return "Username: " + username + ", CurrentElo: " + currentElo + ", NewElo: " + newElo
        + ", MaxElo: " + maxElo;
  }

  public void sendMessage() {
    java.time.Duration showAfter = PGM.get().getConfiguration().showStatsAfter();
    long delay = Math.max(0L, (showAfter.toMillis() + 49L) / 50L) + 1L;
    org.bukkit.Bukkit.getScheduler()
        .runTaskLater(
            TowersForPGM.getInstance(),
            () -> {
              org.bukkit.entity.Player bukkitPlayer = org.bukkit.Bukkit.getPlayerExact(username);
              if (bukkitPlayer == null) return;
              UUID uuid = bukkitPlayer.getUniqueId();
              MatchPlayer player = tc.oc.pgm.api.PGM.get().getMatchManager().getPlayer(uuid);
              int previousElo = newElo - eloChange;
              Rank previousRank = Rank.getRankByElo(previousElo);
              Rank newRank = Rank.getRankByElo(newElo);
              String color = eloChange >= 0
                  ? org.nicolie.towersforpgm.utils.LanguageManager.langMessage(
                      "system.colors.positive")
                  : org.nicolie.towersforpgm.utils.LanguageManager.langMessage(
                      "system.colors.negative");
              String sign = eloChange >= 0 ? "+" : "";
              String white =
                  org.nicolie.towersforpgm.utils.LanguageManager.langMessage("system.colors.white");
              String message;
              if (!previousRank.equals(newRank)) {
                String arrow = newElo > previousElo
                    ? org.nicolie.towersforpgm.utils.LanguageManager.langMessage(
                        "system.colors.arrow.up")
                    : org.nicolie.towersforpgm.utils.LanguageManager.langMessage(
                        "system.colors.arrow.down");
                Sound sound = newElo > previousElo ? Sounds.ADMIN_CHAT : Sounds.ALERT;
                player.playSound(sound);
                message = previousRank.getPrefixedRank(true) + arrow + newRank.getPrefixedRank(true)
                    + " " + white + newElo + " (" + color + sign + eloChange + white + ")";
              } else {
                message = newRank.getPrefixedRank(true) + " " + white + newElo + " (" + color + sign
                    + eloChange + white + ")";
              }
              player.sendMessage(Component.text(message));
            },
            delay);
  }
}
