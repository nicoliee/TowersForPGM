package org.nicolie.towersforpgm.session.draft;

public final class DraftOptions {

  private final boolean randomizeOrder;
  private final boolean allowReroll;
  private final boolean mapVote;
  private final String orderPattern;
  private final int minOrder;

  private DraftOptions(Builder b) {
    this.randomizeOrder = b.randomizeOrder;
    this.allowReroll = b.allowReroll;
    this.mapVote = b.mapVote;
    this.orderPattern = b.orderPattern;
    this.minOrder = b.minOrder;
  }

  public boolean isRandomizeOrder() {
    return randomizeOrder;
  }

  public boolean isAllowReroll() {
    return allowReroll;
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

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private boolean randomizeOrder = true;
    private boolean allowReroll = false;
    private boolean mapVote = false;
    private String orderPattern = "";
    private int minOrder = 0;

    public Builder randomizeOrder(boolean v) {
      this.randomizeOrder = v;
      return this;
    }

    public Builder allowReroll(boolean v) {
      this.allowReroll = v;
      return this;
    }

    public Builder mapVote(boolean v) {
      this.mapVote = v;
      return this;
    }

    public Builder orderPattern(String v) {
      this.orderPattern = v;
      return this;
    }

    public Builder minOrder(int v) {
      this.minOrder = v;
      return this;
    }

    public DraftOptions build() {
      return new DraftOptions(this);
    }
  }
}
