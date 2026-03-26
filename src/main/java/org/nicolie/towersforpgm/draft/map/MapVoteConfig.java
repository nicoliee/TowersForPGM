package org.nicolie.towersforpgm.draft.map;

import java.util.List;

public final class MapVoteConfig {

  public enum VoterMode {
    ALL,
    CAPTAINS_ONLY,
    PLAYERS_ONLY
  }

  public enum VoteMode {
    PLURALITY,
    VETO,
    NONE
  }

  private final List<String> maps;
  private final VoterMode voterMode;
  private final VoteMode voteMode;
  private final int durationSeconds;
  private final boolean showVotes;

  private MapVoteConfig(Builder b) {
    this.maps = List.copyOf(b.maps);
    this.voterMode = b.voterMode;
    this.voteMode = b.voteMode;
    this.durationSeconds = b.durationSeconds;
    this.showVotes = b.showVotes;
  }

  public List<String> getMaps() {
    return maps;
  }

  public VoterMode getVoterMode() {
    return voterMode;
  }

  public VoteMode getVoteMode() {
    return voteMode;
  }

  public int getDuration() {
    return durationSeconds;
  }

  public boolean isShowVotes() {
    return showVotes;
  }

  public boolean isValid() {
    return maps != null && maps.size() >= 2;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private List<String> maps = List.of();
    private VoterMode voterMode = VoterMode.ALL;
    private VoteMode voteMode = VoteMode.PLURALITY;
    private int durationSeconds = 30;
    private boolean showVotes = true;

    public Builder maps(List<String> v) {
      maps = v;
      return this;
    }

    public Builder voterMode(VoterMode v) {
      voterMode = v;
      return this;
    }

    public Builder voteMode(VoteMode v) {
      voteMode = v;
      return this;
    }

    public Builder duration(int v) {
      durationSeconds = v;
      return this;
    }

    public Builder showVotes(boolean v) {
      showVotes = v;
      return this;
    }

    public MapVoteConfig build() {
      return new MapVoteConfig(this);
    }
  }
}
