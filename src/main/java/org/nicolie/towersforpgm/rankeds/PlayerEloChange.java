package org.nicolie.towersforpgm.rankeds;

import java.util.UUID;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.DiscordManager;
import org.nicolie.towersforpgm.matchbot.rankeds.RoleManager;
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
              boolean rankUp = newElo > previousElo;
              boolean changedRank = !previousRank.equals(newRank);
              Sound sound = changedRank ? (rankUp ? Sounds.ADMIN_CHAT : Sounds.ALERT) : null;
              if (sound != null) player.playSound(sound);

              Component message;
              Component eloComponent = Component.text(newElo).color(NamedTextColor.WHITE);
              Component changeComponent = Component.text((eloChange >= 0 ? "+" : "") + eloChange)
                  .color(eloChange >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED);

              if (changedRank) {
                Component arrow = Component.text(" ")
                    .append(Component.text(">> ")
                        .color(rankUp ? NamedTextColor.GREEN : NamedTextColor.RED));
                message = previousRank
                    .getNameComponent(true)
                    .append(arrow)
                    .append(newRank.getNameComponent(true))
                    .append(Component.space())
                    .append(eloComponent)
                    .append(Component.text(" ("))
                    .append(changeComponent)
                    .append(Component.text(")"));
              } else {
                message = newRank
                    .getNameComponent(true)
                    .append(Component.space())
                    .append(eloComponent)
                    .append(Component.text(" ("))
                    .append(changeComponent)
                    .append(Component.text(")"));
              }
              player.sendMessage(message);
            },
            delay);
  }

  public void applyDiscordRoleChange() {
    org.bukkit.entity.Player bukkitPlayer = org.bukkit.Bukkit.getPlayerExact(username);
    if (bukkitPlayer == null) return;
    UUID uuid = bukkitPlayer.getUniqueId();

    int previousElo = newElo - eloChange;
    Rank previousRank = Rank.getRankByElo(previousElo);
    Rank newRank = Rank.getRankByElo(newElo);

    String targetRole = newRank.getRoleID();
    if (targetRole == null) return;

    DiscordManager.getDiscordPlayer(uuid).thenAccept(discordPlayer -> {
      if (discordPlayer == null) return;
      String discordId = discordPlayer.getDiscordId();
      if (discordId == null || discordId.isEmpty()) return;

      RoleManager.changeRole(discordId, previousRank, newRank);
    });
  }

  public String discordFormat() {
    int newRankElo = getNewElo();
    int eloChange = getEloChange();
    String username = getUsername();
    String eloChangeStr = (eloChange >= 0 ? "+" : "") + eloChange;
    String format = Rank.getRankByElo(newRankElo).getPrefixedRank(false) + " " + username + " "
        + newRankElo + " (" + eloChangeStr + ")";
    return format;
  }
}
