package org.nicolie.towersforpgm.configs;

import org.bukkit.plugin.java.JavaPlugin;

public class DraftConfig {
  private final JavaPlugin plugin;
  private boolean draftSuggestions;
  private boolean draftTimer;
  private boolean secondPickBalance;
  private String order;
  private int minOrder;

  public DraftConfig(JavaPlugin plugin) {
    this.plugin = plugin;
    load();
  }

  public void load() {
    draftSuggestions = plugin.getConfig().getBoolean("draft.suggestions", false);
    draftTimer = plugin.getConfig().getBoolean("draft.timer", false);
    secondPickBalance = plugin.getConfig().getBoolean("draft.secondPickBalance", false);
    order = plugin.getConfig().getString("draft.order", "");
    minOrder = plugin.getConfig().getInt("draft.minOrder", 0);
  }

  private void save() {
    plugin.saveConfig();
  }

  public boolean isDraftSuggestions() {
    return draftSuggestions;
  }

  public void setDraftSuggestions(boolean draftSuggestions) {
    this.draftSuggestions = draftSuggestions;
    plugin.getConfig().set("draft.suggestions", draftSuggestions);
    save();
  }

  public boolean isDraftTimer() {
    return draftTimer;
  }

  public void setDraftTimer(boolean draftTimer) {
    this.draftTimer = draftTimer;
    plugin.getConfig().set("draft.timer", draftTimer);
    save();
  }

  public boolean isSecondPickBalance() {
    return secondPickBalance;
  }

  public void setSecondPickBalance(boolean secondPickBalance) {
    this.secondPickBalance = secondPickBalance;
    plugin.getConfig().set("draft.secondPickBalance", secondPickBalance);
    save();
  }

  public String getOrder() {
    return order;
  }

  public void setOrder(String order) {
    this.order = order;
    plugin.getConfig().set("draft.order", order);
    save();
  }

  public int getMinOrder() {
    return minOrder;
  }

  public void setMinOrder(int minOrder) {
    this.minOrder = minOrder;
    plugin.getConfig().set("draft.minOrder", minOrder);
    save();
  }

  @Override
  public String toString() {
    return "DraftConfig{" + "draftSuggestions="
        + draftSuggestions + ", draftTimer="
        + draftTimer + ", secondPickBalance="
        + secondPickBalance + ", order='"
        + order + '\'' + ", minOrder="
        + minOrder + '}';
  }
}
