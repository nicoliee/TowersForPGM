package org.nicolie.towersforpgm.rankeds.queue;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.team.AvailablePlayers;
import org.nicolie.towersforpgm.matchbot.rankeds.listeners.RankedListener;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.session.MatchSession;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.session.ranked.RankedSession;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class QueueManager {

  private final TowersForPGM plugin;
  private final QueueState queueState;
  private final RankedQueue rankedQueue;

  public QueueManager() {
    this.plugin = TowersForPGM.getInstance();
    this.queueState = QueueState.getInstance();
    this.rankedQueue = RankedQueue.getInstance();
  }

  // ── addPlayer ─────────────────────────────────────────────────────────────

  public boolean addPlayer(MatchPlayer player) {
    UUID playerUUID = player.getId();
    Match match = player.getMatch();

    RankedSession ranked = getRankedSession(match);
    AvailablePlayers available = getAvailablePlayers(match);

    if (player.isParticipating()
        || match.isRunning()
        || (ranked != null && ranked.isActive())
        || (available != null && available.isPlayerAvailable(player.getNameLegacy()))) {
      player.sendWarning(Queue.RANKED_PREFIX
          .append(Component.space())
          .append(Component.translatable("join.err.afterStart")));
      return false;
    }

    if (rankedQueue.contains(playerUUID)) {
      player.sendWarning(Queue.RANKED_PREFIX
          .append(Component.space())
          .append(Component.translatable("ranked.alreadyInQueue")));
      return false;
    }

    if (!plugin.config().ranked().isMapRanked(match.getMap().getName())) {
      player.sendWarning(Queue.RANKED_PREFIX
          .append(Component.space())
          .append(Component.translatable(
              "ranked.notRankedMap", Component.text(match.getMap().getName()))));
      return false;
    }

    rankedQueue.add(playerUUID);
    return true;
  }

  public boolean addPlayer(UUID playerUUID, Match match) {
    if (playerUUID == null || rankedQueue.contains(playerUUID)) return false;

    if (match == null) match = MatchManager.getMatch();

    MatchPlayer onlinePlayer = PGM.get().getMatchManager().getPlayer(playerUUID);
    OfflinePlayer offlinePlayer = onlinePlayer != null ? null : Bukkit.getOfflinePlayer(playerUUID);

    if (onlinePlayer == null && (offlinePlayer == null || offlinePlayer.getName() == null)) {
      return false;
    }

    String name = onlinePlayer != null ? onlinePlayer.getNameLegacy() : offlinePlayer.getName();

    if (queueState.isRanked()) {
      AvailablePlayers available = getAvailablePlayers(match);
      if (available != null && available.getAllAvailablePlayers().contains(name)) return false;

      RankedSession ranked = getRankedSession(match);
      if (ranked != null && ranked.isActive()) return false;
    }

    if (!plugin.config().ranked().isMapRanked(match.getMap().getName())) return false;

    rankedQueue.add(playerUUID);
    return true;
  }

  // ── removePlayer ──────────────────────────────────────────────────────────

  public boolean removePlayer(MatchPlayer player) {
    UUID playerUUID = player.getId();

    if (!rankedQueue.contains(playerUUID)) {
      player.sendWarning(Queue.RANKED_PREFIX
          .append(Component.space())
          .append(Component.translatable("ranked.notInQueue")));
      return false;
    }

    rankedQueue.remove(playerUUID);

    if (queueState.isRanked()
        && !queueState.isCountdownActive()
        && rankedQueue.size() < plugin.config().ranked().getRankedMinSize()) {
      queueState.setRanked(false);
    }

    return true;
  }

  public boolean removePlayer(UUID playerUUID) {
    if (playerUUID == null || !rankedQueue.contains(playerUUID)) return false;
    return rankedQueue.remove(playerUUID);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  public List<Component> getQueueDisplayNames() {
    return rankedQueue.snapshot().stream()
        .map(uuid -> MatchManager.getPrefixedName(Bukkit.getOfflinePlayer(uuid).getName()))
        .filter(name -> name != null)
        .collect(Collectors.toList());
  }

  public List<UUID> getDisconnectedPlayersInQueue() {
    return rankedQueue.snapshot().stream()
        .filter(uuid -> PGM.get().getMatchManager().getPlayer(uuid) == null)
        .collect(Collectors.toList());
  }

  public void removeDisconnectedPlayers(Match match, List<UUID> disconnected) {
    for (UUID uuid : disconnected) {
      if (uuid != null && rankedQueue.contains(uuid)) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        rankedQueue.remove(uuid);
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

  private RankedSession getRankedSession(Match match) {
    MatchSession session = MatchSessionRegistry.get(match);
    return session != null ? session.getRanked() : null;
  }

  private AvailablePlayers getAvailablePlayers(Match match) {
    MatchSession session = MatchSessionRegistry.get(match);
    return session != null ? session.availablePlayers() : null;
  }
}
