package org.nicolie.towersforpgm.rankeds;

import java.time.Duration;
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
    Duration durationSeconds = PGM.get().getConfiguration().showStatsAfter();
    long delayTicks = (durationSeconds.getSeconds() * 20L) + 1L;
    org.bukkit.Bukkit.getScheduler()
        .runTaskLater(
            TowersForPGM.getInstance(),
            () -> {
              UUID uuid = org.bukkit.Bukkit.getPlayerExact(username).getUniqueId();
              if (uuid == null) return;
              MatchPlayer player = tc.oc.pgm.api.PGM.get().getMatchManager().getPlayer(uuid);
              int previousElo = newElo - eloChange;
              Rank previousRank = Rank.getRankByElo(previousElo);
              Rank newRank = Rank.getRankByElo(newElo);
              String color = eloChange >= 0 ? "§a" : "§c";
              String sign = eloChange >= 0 ? "+" : "";
              String message;
              if (!previousRank.equals(newRank)) {
                String arrow = newElo > previousElo ? " §a>> " : " §c>> ";
                Sound sound = newElo > previousElo ? Sounds.ADMIN_CHAT : Sounds.ALERT;
                player.playSound(sound);
                message = previousRank.getPrefixedRank(true) + arrow + newRank.getPrefixedRank(true)
                    + " §f" + newElo + " (" + color + sign + eloChange + "§f)";
              } else {
                message = newRank.getPrefixedRank(true) + " §f" + newElo + " (" + color + sign
                    + eloChange + "§f)";
              }
              player.sendMessage(Component.text(message));
            },
            delayTicks);
  }
}
