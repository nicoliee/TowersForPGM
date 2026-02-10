package org.nicolie.towersforpgm.database.models;

public class StatsTransferResult {
  private final boolean success;
  private final String message;
  private final Stats mergedStats;

  public StatsTransferResult(boolean success, String message, Stats mergedStats) {
    this.success = success;
    this.message = message;
    this.mergedStats = mergedStats;
  }

  public static StatsTransferResult success(String message, Stats mergedStats) {
    return new StatsTransferResult(true, message, mergedStats);
  }

  public static StatsTransferResult failure(String message) {
    return new StatsTransferResult(false, message, null);
  }

  public boolean isSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }

  public Stats getMergedStats() {
    return mergedStats;
  }

  @Override
  public String toString() {
    return "StatsTransferResult{" + "success="
        + success + ", message='"
        + message + '\'' + ", mergedStats="
        + mergedStats + '}';
  }
}
