package org.nicolie.towersforpgm.draft.state;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.scheduler.BukkitRunnable;

public class DraftState {

  private DraftPhase currentPhase = DraftPhase.IDLE;
  private BossBar pickTimerBar;
  private BukkitRunnable draftTimer;

  private String customOrderPattern = "";
  private int customOrderMinPlayers = 6;
  private int currentPatternIndex = 0;
  private boolean usingCustomPattern = false;
  private boolean firstCaptainTurn = false;

  public DraftPhase getCurrentPhase() {
    return currentPhase;
  }

  public void setCurrentPhase(DraftPhase phase) {
    this.currentPhase = phase;
  }

  public BossBar getPickTimerBar() {
    return pickTimerBar;
  }

  public void setPickTimerBar(BossBar bar) {
    this.pickTimerBar = bar;
  }

  public BukkitRunnable getDraftTimer() {
    return draftTimer;
  }

  public void setDraftTimer(BukkitRunnable timer) {
    this.draftTimer = timer;
  }

  public String getCustomOrderPattern() {
    return customOrderPattern;
  }

  public void setCustomOrderPattern(String p) {
    this.customOrderPattern = p;
  }

  public int getCustomOrderMinPlayers() {
    return customOrderMinPlayers;
  }

  public void setCustomOrderMinPlayers(int n) {
    this.customOrderMinPlayers = n;
  }

  public int getCurrentPatternIndex() {
    return currentPatternIndex;
  }

  public void setCurrentPatternIndex(int i) {
    this.currentPatternIndex = i;
  }

  public boolean isUsingCustomPattern() {
    return usingCustomPattern;
  }

  public void setUsingCustomPattern(boolean v) {
    this.usingCustomPattern = v;
  }

  public boolean isFirstCaptainTurn() {
    return firstCaptainTurn;
  }

  public void setFirstCaptainTurn(boolean v) {
    this.firstCaptainTurn = v;
  }

  public void resetPattern() {
    usingCustomPattern = false;
    customOrderPattern = "";
    customOrderMinPlayers = 6;
    currentPatternIndex = 0;
    firstCaptainTurn = false;
  }

  public void reset() {
    resetPattern();
    currentPhase = DraftPhase.IDLE;
    pickTimerBar = null;
    draftTimer = null;
  }
}
