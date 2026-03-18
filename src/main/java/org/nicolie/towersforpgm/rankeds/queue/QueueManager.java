package org.nicolie.towersforpgm.rankeds.queue;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.team.AvailablePlayers;
import org.nicolie.towersforpgm.draft.team.Teams;
import org.nicolie.towersforpgm.matchbot.rankeds.listeners.RankedListener;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.session.MatchSession;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class QueueManager {

  private final TowersForPGM plugin;
  private final QueueState queueState;

  // ── Constructor — no longer receives Teams ────────────────────────────────

  public QueueManager() {
    this.plugin = TowersForPGM.getInstance();
    this.queueState = QueueState.getInstance();
  }

  // ── Helpers — resolve session state per match ─────────────────────────────

  /**
   * Returns the active Teams for {@code match}, or {@code null} if no draft/matchmaking has been
   * started yet.
   */
  private Teams teamsFor(Match match) {
    MatchSession session = MatchSessionRegistry.get(match);
    return session != null ? session.teams() : null;
  }

  /** Returns the active AvailablePlayers for {@code match}, or {@code null}. */
  private AvailablePlayers availablePlayersFor(Match match) {
    MatchSession session = MatchSessionRegistry.get(match);
    return session != null ? session.availablePlayers() : null;
  }

  // ── addPlayer (MatchPlayer overload) ──────────────────────────────────────

  public boolean addPlayer(MatchPlayer player) {
    UUID playerUUID = player.getId();
    Match match = player.getMatch();
    String map = match.getMap().getName();

    // Check if player is already in a team for this match
    Teams teams = teamsFor(match);
    if (player.isParticipating()
        || match.isRunning()
        || (teams != null && teams.isPlayerInAnyTeam(player.getNameLegacy()))) {
      player.sendWarning(Queue.RANKED_PREFIX
          .append(Component.space())
          .append(Component.translatable("join.err.afterStart")));
      return false;
    }

    if (queueState.containsPlayer(playerUUID)) {
      player.sendWarning(Queue.RANKED_PREFIX
          .append(Component.space())
          .append(Component.translatable("ranked.alreadyInQueue")));
      return false;
    }

    if (!plugin.config().ranked().isMapRanked(map)) {
      player.sendWarning(Queue.RANKED_PREFIX
          .append(Component.space())
          .append(Component.translatable("ranked.notRankedMap"), Component.text(map)));
      return false;
    }

    queueState.addPlayer(playerUUID);
    return true;
  }

  // ── addPlayer (UUID overload — used by voice queue) ───────────────────────

  public boolean addPlayer(UUID playerUUID, Match match) {
    if (playerUUID == null || queueState.containsPlayer(playerUUID)) return false;

    if (match == null) match = getDefaultMatch();

    MatchPlayer onlinePlayer = PGM.get().getMatchManager().getPlayer(playerUUID);
    OfflinePlayer offlinePlayer = onlinePlayer != null ? null : Bukkit.getOfflinePlayer(playerUUID);

    if (onlinePlayer == null && (offlinePlayer == null || offlinePlayer.getName() == null)) {
      return false;
    }

    String name = onlinePlayer != null ? onlinePlayer.getNameLegacy() : offlinePlayer.getName();

    // When a ranked match is already running, block players that are
    // already assigned to a team or in the draft pool.
    if (queueState.isRanked()) {
      AvailablePlayers available = availablePlayersFor(match);
      if (available != null && available.getAllAvailablePlayers().contains(name)) {
        return false;
      }

      Teams teams = teamsFor(match);
      if (teams != null && teams.isPlayerInAnyTeam(name)) {
        return false;
      }
    }

    if (!plugin.config().ranked().isMapRanked(match.getMap().getName())) return false;

    queueState.addPlayer(playerUUID);
    return true;
  }

  // ── removePlayer ──────────────────────────────────────────────────────────

  public boolean removePlayer(MatchPlayer player) {
    UUID playerUUID = player.getId();

    if (!queueState.containsPlayer(playerUUID)) {
      player.sendWarning(Queue.RANKED_PREFIX
          .append(Component.space())
          .append(Component.translatable("ranked.notInQueue")));
      return false;
    }

    queueState.removePlayer(playerUUID);

    if (queueState.isRanked()
        && !queueState.isCountdownActive()
        && queueState.getQueueSize() < plugin.config().ranked().getRankedMinSize()) {
      queueState.setRanked(false);
    }

    return true;
  }

  public boolean removePlayer(UUID playerUUID) {
    if (playerUUID == null || !queueState.containsPlayer(playerUUID)) return false;
    return queueState.removePlayer(playerUUID);
  }

  // ── Unchanged methods ─────────────────────────────────────────────────────

  public List<Component> getQueueDisplayNames() {
    return queueState.getQueuePlayers().stream()
        .map(uuid -> {
          OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
          return MatchManager.getPrefixedName(op.getName());
        })
        .filter(name -> name != null)
        .collect(Collectors.toList());
  }

  public List<UUID> getDisconnectedPlayersInQueue() {
    return queueState.getQueuePlayers().stream()
        .filter(uuid -> PGM.get().getMatchManager().getPlayer(uuid) == null)
        .collect(Collectors.toList());
  }

  public void removeDisconnectedPlayers(Match match, List<UUID> disconnected) {
    for (UUID uuid : disconnected) {
      if (uuid != null && queueState.containsPlayer(uuid)) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        queueState.removePlayer(uuid);
        RankedListener.movePlayerToInactive(uuid);
        Queue.getQueueMessaging().sendQueueMessage(match, name, true);
      }
    }
  }

  public UUID getUUIDFromUsername(String username) {
    OfflinePlayer offline = Bukkit.getPlayerExact(username);
    return offline != null ? offline.getUniqueId() : null;
  }

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
}
