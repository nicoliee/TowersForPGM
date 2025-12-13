package org.nicolie.towersforpgm.rankeds.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.scheduler.BukkitTask;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;

public class QueueState {
  private static QueueState instance;

  private boolean ranked = false;
  private boolean countdownActive = false;
  private BukkitTask countdownTask = null;
  private final List<UUID> queuePlayers = new ArrayList<>();
  private final Map<String, java.util.concurrent.CompletableFuture<List<PlayerEloChange>>>
      eloCache = new ConcurrentHashMap<>();

  private QueueState() {}

  public static QueueState getInstance() {
    if (instance == null) {
      instance = new QueueState();
    }
    return instance;
  }

  public boolean isRanked() {
    return ranked;
  }

  public void setRanked(boolean ranked) {
    this.ranked = ranked;
  }

  public boolean isCountdownActive() {
    return countdownActive;
  }

  public void setCountdownActive(boolean countdownActive) {
    this.countdownActive = countdownActive;
  }

  public BukkitTask getCountdownTask() {
    return countdownTask;
  }

  public void setCountdownTask(BukkitTask countdownTask) {
    this.countdownTask = countdownTask;
  }

  // Queue players management
  public List<UUID> getQueuePlayers() {
    return new ArrayList<>(queuePlayers);
  }

  public boolean containsPlayer(UUID playerUUID) {
    return queuePlayers.contains(playerUUID);
  }

  public void addPlayer(UUID playerUUID) {
    if (!queuePlayers.contains(playerUUID)) {
      queuePlayers.add(playerUUID);
    }
  }

  public boolean removePlayer(UUID playerUUID) {
    return queuePlayers.remove(playerUUID);
  }

  public int getQueueSize() {
    return queuePlayers.size();
  }

  public void clearQueue() {
    queuePlayers.clear();
  }

  public Map<String, java.util.concurrent.CompletableFuture<List<PlayerEloChange>>> getEloCache() {
    return eloCache;
  }

  public void putEloCache(
      String table, java.util.concurrent.CompletableFuture<List<PlayerEloChange>> future) {
    eloCache.put(table, future);
  }

  public java.util.concurrent.CompletableFuture<List<PlayerEloChange>> getEloCacheOrDefault(
      String table, java.util.concurrent.CompletableFuture<List<PlayerEloChange>> defaultValue) {
    return eloCache.getOrDefault(table, defaultValue);
  }

  public void removeEloCache(String table) {
    eloCache.remove(table);
  }

  public void reset() {
    ranked = false;
    countdownActive = false;
    if (countdownTask != null) {
      countdownTask.cancel();
      countdownTask = null;
    }
    queuePlayers.clear();
    eloCache.clear();
  }

  public void cancelCountdown() {
    if (countdownTask != null) {
      countdownTask.cancel();
      countdownTask = null;
    }
    countdownActive = false;
  }
}
