package org.nicolie.towersforpgm.rankeds.queue;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Teams;
import org.nicolie.towersforpgm.matchbot.rankeds.listeners.RankedListener;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

/**
 * Handles player-related queue operations such as adding/removing players and managing player
 * validation logic.
 */
public class QueueManager {
  private final TowersForPGM plugin;
  private final QueueState queueState;
  private final Teams teams;

  public QueueManager(Teams teams) {
    this.plugin = TowersForPGM.getInstance();
    this.queueState = QueueState.getInstance();
    this.teams = teams;
  }

  /** Adds a player to the queue with full validation. */
  public boolean addPlayer(MatchPlayer player) {
    UUID playerUUID = player.getId();
    Match match = player.getMatch();
    String map = match.getMap().getName();

    if (player.isParticipating()
        || match.isRunning()
        || teams.isPlayerInAnyTeam(player.getNameLegacy())) {
      player.sendWarning(
          Component.text(getRankedPrefix() + LanguageManager.message("ranked.matchInProgress")));
      return false;
    }

    if (queueState.containsPlayer(playerUUID)) {
      player.sendWarning(
          Component.text(getRankedPrefix() + LanguageManager.message("ranked.alreadyInQueue")));
      return false;
    }

    if (!plugin.config().ranked().isMapRanked(map)) {
      player.sendWarning(Component.text(getRankedPrefix()
          + LanguageManager.message("ranked.notRankedMap").replace("{map}", map)));
      return false;
    }

    queueState.addPlayer(playerUUID);
    return true;
  }

  /** Adds a player to the queue by UUID with validation. */
  public boolean addPlayer(UUID playerUUID, Match match) {
    if (playerUUID == null || queueState.containsPlayer(playerUUID)) {
      return false;
    }

    if (match == null) {
      match = getDefaultMatch();
    }

    MatchPlayer onlinePlayer = PGM.get().getMatchManager().getPlayer(playerUUID);
    OfflinePlayer offlinePlayer = onlinePlayer != null ? null : Bukkit.getOfflinePlayer(playerUUID);

    if (onlinePlayer == null && (offlinePlayer == null || offlinePlayer.getName() == null)) {
      return false;
    }

    String name = onlinePlayer != null ? onlinePlayer.getNameLegacy() : offlinePlayer.getName();

    if (queueState.isRanked()) {
      AvailablePlayers available = plugin.getAvailablePlayers();
      if (available != null) {
        List<String> all = available.getAllAvailablePlayers();
        if (all.contains(name)) {
          return false;
        }
      }

      if (teams != null && teams.isPlayerInAnyTeam(name)) {
        return false;
      }
    }

    String map = match.getMap().getName();
    if (!plugin.config().ranked().isMapRanked(map)) {
      return false;
    }

    queueState.addPlayer(playerUUID);
    return true;
  }

  /** Removes a player from the queue. */
  public boolean removePlayer(MatchPlayer player) {
    UUID playerUUID = player.getId();

    if (!queueState.containsPlayer(playerUUID)) {
      player.sendWarning(
          Component.text(getRankedPrefix() + LanguageManager.message("ranked.notInQueue")));
      return false;
    }

    queueState.removePlayer(playerUUID);

    // Reset ranked if queue becomes too small and no countdown is active
    if (queueState.isRanked()
        && !queueState.isCountdownActive()
        && queueState.getQueueSize() < plugin.config().ranked().getRankedMinSize()) {
      queueState.setRanked(false);
    }

    return true;
  }

  /** Removes a player from the queue by UUID. */
  public boolean removePlayer(UUID playerUUID) {
    if (playerUUID == null || !queueState.containsPlayer(playerUUID)) {
      return false;
    }

    return queueState.removePlayer(playerUUID);
  }

  /** Processes voice join logic for a player. */
  public void processVoiceJoin(UUID playerUUID, Match match) {
    if (playerUUID == null) return;

    if (match == null) {
      match = getDefaultMatch();
    }

    if (!queueState.isRanked()) {
      addPlayer(playerUUID, match);
      return;
    }

    MatchPlayer onlinePlayer = PGM.get().getMatchManager().getPlayer(playerUUID);
    String plainName = onlinePlayer != null
        ? onlinePlayer.getNameLegacy()
        : Bukkit.getOfflinePlayer(playerUUID).getName();

    if (plainName == null) return;

    if (teams != null && teams.isPlayerInAnyTeam(plainName)) {
      if (teams.isPlayerInTeam(plainName, 1)) {
        RankedListener.movePlayerToTeam1(playerUUID);
      } else {
        RankedListener.movePlayerToTeam2(playerUUID);
      }
      return;
    }

    addPlayer(playerUUID, match);
  }

  /** Gets list of player names in queue for display. */
  public List<String> getQueueDisplayNames() {
    return queueState.getQueuePlayers().stream()
        .map(uuid -> {
          MatchPlayer player = PGM.get().getMatchManager().getPlayer(uuid);
          if (player != null) {
            return player.getPrefixedName();
          } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            return offlinePlayer.getName() != null ? "§3" + offlinePlayer.getName() : null;
          }
        })
        .filter(name -> name != null)
        .collect(Collectors.toList());
  }

  /** Gets list of disconnected players in queue. */
  public List<UUID> getDisconnectedPlayersInQueue() {
    return queueState.getQueuePlayers().stream()
        .filter(uuid -> PGM.get().getMatchManager().getPlayer(uuid) == null)
        .collect(Collectors.toList());
  }

  /** Removes disconnected players from queue. */
  public void removeDisconnectedPlayers(Match match, List<UUID> disconnectedPlayerUUIDs) {
    for (UUID playerUUID : disconnectedPlayerUUIDs) {
      if (playerUUID != null && queueState.containsPlayer(playerUUID)) {
        queueState.removePlayer(playerUUID);
        RankedListener.movePlayerToInactive(playerUUID);
      }
    }
  }

  /** Gets UUID from username. */
  public UUID getUUIDFromUsername(String username) {
    OfflinePlayer offline = Bukkit.getPlayerExact(username);
    return offline != null ? offline.getUniqueId() : null;
  }

  /** Gets valid ranked size based on match and configuration. */
  public int getValidRankedSize(Match match) {
    int minSize = plugin.config().ranked().getRankedMinSize();
    int maxSize = plugin.config().ranked().getRankedMaxSize();
    int target = Math.min(maxSize, match.getPlayers().size());
    if (target % 2 != 0) target--;
    return target >= minSize ? target : minSize;
  }

  private Match getDefaultMatch() {
    return org.nicolie.towersforpgm.utils.MatchManager.getMatch();
  }

  private String getRankedPrefix() {
    return LanguageManager.message("ranked.prefix");
  }
}
