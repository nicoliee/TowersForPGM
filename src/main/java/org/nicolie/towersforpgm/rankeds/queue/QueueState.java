package org.nicolie.towersforpgm.rankeds.queue;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;

public final class QueueState {

  private static final QueueState INSTANCE = new QueueState();

  private boolean ranked = false;
  private boolean countdownActive = false;
  private ScheduledFuture<?> countdownFuture = null;

  private final Map<String, java.util.concurrent.CompletableFuture<List<PlayerEloChange>>>
      eloCache = new ConcurrentHashMap<>();

  private QueueState() {}

  public static QueueState getInstance() {
    return INSTANCE;
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

  public ScheduledFuture<?> getCountdownFuture() {
    return countdownFuture;
  }

  public void setCountdownFuture(ScheduledFuture<?> future) {
    this.countdownFuture = future;
  }

  public int getQueueSize() {
    return RankedQueue.getInstance().size();
  }

  public List<UUID> getQueuePlayers() {
    return RankedQueue.getInstance().snapshot();
  }

  public boolean containsPlayer(UUID uuid) {
    return RankedQueue.getInstance().contains(uuid);
  }

  public void addPlayer(UUID uuid) {
    RankedQueue.getInstance().add(uuid);
  }

  public boolean removePlayer(UUID uuid) {
    return RankedQueue.getInstance().remove(uuid);
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

  public void cancelCountdown() {
    if (countdownFuture != null) {
      countdownFuture.cancel(false);
      countdownFuture = null;
    }
    countdownActive = false;
  }

  public void reset() {
    ranked = false;
    cancelCountdown();
    RankedQueue.getInstance().clear();
    eloCache.clear();
  }

  public java.util.Map<
          String,
          java.util.concurrent.CompletableFuture<
              java.util.List<org.nicolie.towersforpgm.rankeds.PlayerEloChange>>>
      snapshotEloCache() {
    return new java.util.HashMap<>(eloCache);
  }
}
