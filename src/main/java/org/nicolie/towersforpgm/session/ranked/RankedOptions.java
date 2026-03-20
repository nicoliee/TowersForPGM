package org.nicolie.towersforpgm.session.ranked;

public final class RankedOptions {

  private final boolean matchmaking;
  private final String orderPattern;
  private final int minOrder;
  private final boolean allowReroll;
  private final boolean randomizeOrder;

  private RankedOptions(Builder b) {
    this.matchmaking = b.matchmaking;
    this.orderPattern = b.orderPattern;
    this.minOrder = b.minOrder;
    this.allowReroll = b.allowReroll;
    this.randomizeOrder = b.randomizeOrder;
  }

  public boolean isMatchmaking() {
    return matchmaking;
  }

  public String getOrderPattern() {
    return orderPattern;
  }

  public int getMinOrder() {
    return minOrder;
  }

  public boolean isAllowReroll() {
    return allowReroll;
  }

  public boolean isRandomizeOrder() {
    return randomizeOrder;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private boolean matchmaking = false;
    private String orderPattern = "";
    private int minOrder = 0;
    private boolean allowReroll = false;
    private boolean randomizeOrder = true;

    public Builder matchmaking(boolean v) {
      this.matchmaking = v;
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

    public Builder allowReroll(boolean v) {
      this.allowReroll = v;
      return this;
    }

    public Builder randomizeOrder(boolean v) {
      this.randomizeOrder = v;
      return this;
    }

    public RankedOptions build() {
      return new RankedOptions(this);
    }
  }
}
