package org.nicolie.towersforpgm.session.draft;

import java.util.List;
import javax.annotation.Nullable;
import org.nicolie.towersforpgm.draft.map.MapVoteConfig;
import org.nicolie.towersforpgm.draft.map.MapVoteConfig.VoteMode;
import org.nicolie.towersforpgm.draft.map.MapVoteConfig.VoterMode;

public final class DraftOptions {

  private final boolean randomizeOrder;
  private final boolean mapVote;
  private final String orderPattern;
  private final int minOrder;

  @Nullable
  private final MapVoteConfig mapVoteConfig;

  private DraftOptions(Builder b) {
    this.randomizeOrder = b.randomizeOrder;
    this.mapVote = b.mapVote;
    this.orderPattern = b.orderPattern;
    this.minOrder = b.minOrder;
    this.mapVoteConfig = b.mapVoteConfig;
  }

  public boolean isRandomizeOrder() {
    return randomizeOrder;
  }

  public boolean isMapVote() {
    return mapVote;
  }

  public String getOrderPattern() {
    return orderPattern;
  }

  public int getMinOrder() {
    return minOrder;
  }

  @Nullable
  public MapVoteConfig getMapVoteConfig() {
    return mapVoteConfig;
  }

  public boolean hasMapVote() {
    return mapVoteConfig != null && mapVoteConfig.isValid();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private boolean randomizeOrder = true;
    private boolean mapVote = false;
    private String orderPattern = "";
    private int minOrder = 0;

    @Nullable
    private MapVoteConfig mapVoteConfig = null;

    public Builder randomizeOrder(boolean v) {
      randomizeOrder = v;
      return this;
    }

    public Builder mapVote(boolean v) {
      mapVote = v;
      return this;
    }

    public Builder orderPattern(String v) {
      orderPattern = v;
      return this;
    }

    public Builder minOrder(int v) {
      minOrder = v;
      return this;
    }

    public Builder mapVoteConfig(MapVoteConfig v) {
      mapVoteConfig = v;
      return this;
    }

    public Builder maps(List<String> maps, VoterMode voterMode, VoteMode voteMode) {
      return maps(maps, voterMode, voteMode, 30);
    }

    public Builder maps(List<String> maps, VoterMode voterMode, VoteMode voteMode, int duration) {
      if (maps != null && maps.size() >= 2) {
        this.mapVoteConfig = MapVoteConfig.builder()
            .maps(maps)
            .voterMode(voterMode)
            .voteMode(voteMode)
            .duration(duration)
            .showVotes(true)
            .build();
      }
      return this;
    }

    public DraftOptions build() {
      return new DraftOptions(this);
    }
  }
}
