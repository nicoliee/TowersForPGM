package org.nicolie.towersforpgm.matchbot.cache;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.nicolie.towersforpgm.database.models.top.Top;

public class TopLRUCache {
  private static final int MAX_ENTRIES = 256;
  private static final long TTL_MILLIS = 15_000; // 15s
  private static final long GLOBAL_FLUSH_INTERVAL = 30_000; 
  private static volatile long lastGlobalFlush = System.currentTimeMillis();

  private static final Map<String, Entry> CACHE =
      new LinkedHashMap<String, Entry>(128, 0.75f, true) {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
          return size() > MAX_ENTRIES;
        }
      };

  private static final ConcurrentHashMap<String, Object> LOCKS = new ConcurrentHashMap<>();

  private static class Entry {
    final List<Top> data;
    final long time;
    final int totalRecords;
    final double lastValue;
    final String lastUser;

    Entry(List<Top> data, long time, int totalRecords, double lastValue, String lastUser) {
      this.data = data;
      this.time = time;
      this.totalRecords = totalRecords;
      this.lastValue = lastValue;
      this.lastUser = lastUser;
    }
  }

  private static String key(String table, String column, int page, int limit, boolean perGame) {
    return table + '|' + column + '|' + page + '|' + limit + '|' + (perGame ? '1' : '0');
  }

  public static CachedPage get(String table, String column, int page, int limit, boolean perGame) {
    clearIfExpired();
    synchronized (CACHE) {
      Entry e = CACHE.get(key(table, column, page, limit, perGame));
      if (e == null) return null;
      if (System.currentTimeMillis() - e.time > TTL_MILLIS) {
        CACHE.remove(key(table, column, page, limit, perGame));
        return null;
      }
      return new CachedPage(e.data, e.totalRecords, e.lastValue, e.lastUser);
    }
  }

  public static void put(
      String table,
      String column,
      int page,
      int limit,
      boolean perGame,
      List<Top> data,
      int totalRecords,
      double lastValue,
      String lastUser) {
    clearIfExpired();
    synchronized (CACHE) {
      CACHE.put(
          key(table, column, page, limit, perGame),
          new Entry(data, System.currentTimeMillis(), totalRecords, lastValue, lastUser));
    }
  }

  public static CompletableFuture<CachedPage> getOrLoad(
      String table,
      String column,
      int page,
      int limit,
      boolean perGame,
      Supplier<CompletableFuture<CachedPage>> supplier) {
    CachedPage cached = get(table, column, page, limit, perGame);
    if (cached != null) return CompletableFuture.completedFuture(cached);
    Object lock =
        LOCKS.computeIfAbsent(key(table, column, page, limit, perGame), k -> new Object());
    synchronized (lock) {
      cached = get(table, column, page, limit, perGame);
      if (cached != null) return CompletableFuture.completedFuture(cached);
      return supplier.get().thenApply(loaded -> {
        if (loaded != null) {
          put(
              table,
              column,
              page,
              limit,
              perGame,
              loaded.data(),
              loaded.totalRecords(),
              loaded.lastValue(),
              loaded.lastUser());
        }
        return loaded;
      });
    }
  }

  private static void clearIfExpired() {
    long now = System.currentTimeMillis();
    if (now - lastGlobalFlush >= GLOBAL_FLUSH_INTERVAL) {
      synchronized (CACHE) {
        if (now - lastGlobalFlush >= GLOBAL_FLUSH_INTERVAL) {
          CACHE.clear();
          lastGlobalFlush = now;
        }
      }
    }
  }

  public static void clear() {
    synchronized (CACHE) {
      CACHE.clear();
      lastGlobalFlush = System.currentTimeMillis();
    }
    LOCKS.clear();
  }

  public static class CachedPage {
    private final List<Top> data;
    private final int totalRecords;
    private final double lastValue;
    private final String lastUser;

    public CachedPage(List<Top> data, int totalRecords, double lastValue, String lastUser) {
      this.data = data;
      this.totalRecords = totalRecords;
      this.lastValue = lastValue;
      this.lastUser = lastUser;
    }

    public List<Top> data() {
      return data;
    }

    public int totalRecords() {
      return totalRecords;
    }

    public double lastValue() {
      return lastValue;
    }

    public String lastUser() {
      return lastUser;
    }
  }
}
