package org.nicolie.towersforpgm.session.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.nicolie.towersforpgm.draft.state.DraftPhase;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;

public final class CrossMatchSnapshot {

  private final DraftPhase draftPhase;

  private final UUID captain1;
  private final UUID captain2;
  private final boolean captain1Turn;
  private final boolean firstCaptainTurn;
  private final List<Map.Entry<String, Integer>> pickHistory;
  private final List<String> remainingPool;
  private final String orderPattern;
  private final int orderMinPlayers;
  private final int patternIndex;
  private final boolean usingCustomPattern;
  private final boolean allowReroll;
  private final boolean ranked;
  private String tempTable;
  private final List<UUID> rankedPlayers;

  @Nullable
  private final Map<String, CompletableFuture<List<PlayerEloChange>>> eloCache;

  private CrossMatchSnapshot(Builder b) {
    this.draftPhase = b.draftPhase;
    this.captain1 = b.captain1;
    this.captain2 = b.captain2;
    this.captain1Turn = b.captain1Turn;
    this.firstCaptainTurn = b.firstCaptainTurn;
    this.pickHistory = List.copyOf(b.pickHistory);
    this.remainingPool = List.copyOf(b.remainingPool);
    this.orderPattern = b.orderPattern;
    this.orderMinPlayers = b.orderMinPlayers;
    this.patternIndex = b.patternIndex;
    this.usingCustomPattern = b.usingCustomPattern;
    this.allowReroll = b.allowReroll;
    this.ranked = b.ranked;
    this.rankedPlayers = List.copyOf(b.rankedPlayers);
    this.eloCache = b.eloCache;
    this.tempTable = b.tempTable;
  }

  public DraftPhase getDraftPhase() {
    return draftPhase;
  }

  @Nullable
  public UUID getCaptain1() {
    return captain1;
  }

  @Nullable
  public UUID getCaptain2() {
    return captain2;
  }

  public boolean isCaptain1Turn() {
    return captain1Turn;
  }

  public boolean isFirstCaptainTurn() {
    return firstCaptainTurn;
  }

  public List<Map.Entry<String, Integer>> getPickHistory() {
    return pickHistory;
  }

  public List<String> getRemainingPool() {
    return remainingPool;
  }

  public String getOrderPattern() {
    return orderPattern;
  }

  public int getOrderMinPlayers() {
    return orderMinPlayers;
  }

  public int getPatternIndex() {
    return patternIndex;
  }

  public boolean isUsingCustomPattern() {
    return usingCustomPattern;
  }

  public boolean isAllowReroll() {
    return allowReroll;
  }

  public boolean isRanked() {
    return ranked;
  }

  public List<UUID> getRankedPlayers() {
    return rankedPlayers;
  }

  @Nullable
  public Map<String, CompletableFuture<List<PlayerEloChange>>> getEloCache() {
    return eloCache;
  }

  public String getTempTable() {
    return tempTable;
  }

  public boolean hasDraftState() {
    return captain1 != null && captain2 != null && draftPhase != DraftPhase.IDLE;
  }

  public boolean hasRankedPlayers() {
    return ranked && !rankedPlayers.isEmpty();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private DraftPhase draftPhase = DraftPhase.IDLE;
    private UUID captain1;
    private UUID captain2;
    private boolean captain1Turn = true;
    private boolean firstCaptainTurn = true;
    private final List<Map.Entry<String, Integer>> pickHistory = new ArrayList<>();
    private final List<String> remainingPool = new ArrayList<>();
    private String orderPattern = "";
    private int orderMinPlayers = 0;
    private int patternIndex = 0;
    private boolean usingCustomPattern = false;
    private boolean allowReroll = false;
    private boolean ranked = false;
    private String tempTable;
    private final List<UUID> rankedPlayers = new ArrayList<>();
    private Map<String, CompletableFuture<List<PlayerEloChange>>> eloCache;

    public Builder draftPhase(DraftPhase v) {
      draftPhase = v;
      return this;
    }

    public Builder captain1(UUID v) {
      captain1 = v;
      return this;
    }

    public Builder captain2(UUID v) {
      captain2 = v;
      return this;
    }

    public Builder captain1Turn(boolean v) {
      captain1Turn = v;
      return this;
    }

    public Builder firstCaptainTurn(boolean v) {
      firstCaptainTurn = v;
      return this;
    }

    public Builder orderPattern(String v) {
      orderPattern = v == null ? "" : v;
      return this;
    }

    public Builder orderMinPlayers(int v) {
      orderMinPlayers = v;
      return this;
    }

    public Builder patternIndex(int v) {
      patternIndex = v;
      return this;
    }

    public Builder usingCustomPattern(boolean v) {
      usingCustomPattern = v;
      return this;
    }

    public Builder allowReroll(boolean v) {
      allowReroll = v;
      return this;
    }

    public Builder ranked(boolean v) {
      ranked = v;
      return this;
    }

    public Builder tempTable(String v) {
      tempTable = v;
      return this;
    }

    public Builder eloCache(Map<String, CompletableFuture<List<PlayerEloChange>>> v) {
      eloCache = v;
      return this;
    }

    public Builder addPickHistory(List<Map.Entry<String, Integer>> list) {
      pickHistory.addAll(list);
      return this;
    }

    public Builder addRemainingPlayers(List<String> list) {
      remainingPool.addAll(list);
      return this;
    }

    public Builder addRankedPlayers(List<UUID> list) {
      rankedPlayers.addAll(list);
      return this;
    }

    public CrossMatchSnapshot build() {
      return new CrossMatchSnapshot(this);
    }
  }
}
