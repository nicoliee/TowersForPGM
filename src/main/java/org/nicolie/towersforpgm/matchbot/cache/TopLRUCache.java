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

  private static final Map<String, CacheEntry> CACHE =
      new LinkedHashMap<String, CacheEntry>(128, 0.75f, true) {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
          return size() > MAX_ENTRIES;
        }
      };

  private static final ConcurrentHashMap<String, Object> LOCKS = new ConcurrentHashMap<>();

  private static class CacheEntry {
    final List<Top> data;
    final int totalRecords;
    final double lastValue;
    final String lastUser;

    CacheEntry(List<Top> data, int totalRecords, double lastValue, String lastUser) {
      this.data = data;
      this.totalRecords = totalRecords;
      this.lastValue = lastValue;
      this.lastUser = lastUser;
    }
  }

  private static String key(String table, String column, int page, int limit, boolean perGame) {
    return table + '|' + column + '|' + page + '|' + limit + '|' + (perGame ? '1' : '0');
  }

  public static CachedPage get(String table, String column, int page, int limit, boolean perGame) {
    synchronized (CACHE) {
      CacheEntry e = CACHE.get(key(table, column, page, limit, perGame));
      if (e == null) return null;
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
    synchronized (CACHE) {
      CACHE.put(
          key(table, column, page, limit, perGame),
          new CacheEntry(data, totalRecords, lastValue, lastUser));
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
    Object lock = LOCKS.computeIfAbsent(key(table, column, page, limit, perGame), k -> new Object());
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

  public static void clear() {
    synchronized (CACHE) {
      CACHE.clear();
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

