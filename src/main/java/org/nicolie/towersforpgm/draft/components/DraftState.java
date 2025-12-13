package org.nicolie.towersforpgm.draft.components;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.scheduler.BukkitRunnable;

public class DraftState {
  private boolean draftActive = false;
  private BossBar pickTimerBar;
  private BukkitRunnable draftTimer;

  private String customOrderPattern = "";
  private int customOrderMinPlayers = 6;
  private int currentPatternIndex = 0;
  private boolean usingCustomPattern = false;
  private boolean firstCaptainTurn = false;

  public boolean isDraftActive() {
    return draftActive;
  }

  public void setDraftActive(boolean draftActive) {
    this.draftActive = draftActive;
  }

  public BossBar getPickTimerBar() {
    return pickTimerBar;
  }

  public void setPickTimerBar(BossBar pickTimerBar) {
    this.pickTimerBar = pickTimerBar;
  }

  public BukkitRunnable getDraftTimer() {
    return draftTimer;
  }

  public void setDraftTimer(BukkitRunnable draftTimer) {
    this.draftTimer = draftTimer;
  }

  public String getCustomOrderPattern() {
    return customOrderPattern;
  }

  public void setCustomOrderPattern(String customOrderPattern) {
    this.customOrderPattern = customOrderPattern;
  }

  public int getCustomOrderMinPlayers() {
    return customOrderMinPlayers;
  }

  public void setCustomOrderMinPlayers(int customOrderMinPlayers) {
    this.customOrderMinPlayers = customOrderMinPlayers;
  }

  public int getCurrentPatternIndex() {
    return currentPatternIndex;
  }

  public void setCurrentPatternIndex(int currentPatternIndex) {
    this.currentPatternIndex = currentPatternIndex;
  }

  public boolean isUsingCustomPattern() {
    return usingCustomPattern;
  }

  public void setUsingCustomPattern(boolean usingCustomPattern) {
    this.usingCustomPattern = usingCustomPattern;
  }

  public boolean isFirstCaptainTurn() {
    return firstCaptainTurn;
  }

  public void setFirstCaptainTurn(boolean firstCaptainTurn) {
    this.firstCaptainTurn = firstCaptainTurn;
  }

  public void resetPattern() {
    this.usingCustomPattern = false;
    this.customOrderPattern = "";
    this.customOrderMinPlayers = 6;
    this.currentPatternIndex = 0;
    this.firstCaptainTurn = false;
  }
}
