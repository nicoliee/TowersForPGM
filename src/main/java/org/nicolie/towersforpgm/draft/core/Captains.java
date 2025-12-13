package org.nicolie.towersforpgm.draft.core;

import java.time.Duration;
import java.util.UUID;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.start.StartMatchModule;

public class Captains {
  private UUID captain1 = null;
  private UUID captain2 = null;
  private boolean ready1 = false;
  private boolean ready2 = false;
  private boolean isReadyActive = false;
  private boolean isMatchWithCaptains = false;
  private boolean isCaptain1Turn = true;

  public UUID getCaptain1() {
    return captain1;
  }

  public String getCaptain1Name() {
    return Bukkit.getPlayer(captain1) != null ? Bukkit.getPlayer(captain1).getName() : null;
  }

  public void setCaptain1(UUID captain1) {
    this.captain1 = captain1;
  }

  public UUID getCaptain2() {
    return captain2;
  }

  public String getCaptain2Name() {
    return Bukkit.getPlayer(captain2) != null ? Bukkit.getPlayer(captain2).getName() : null;
  }

  public void setCaptain2(UUID captain2) {
    this.captain2 = captain2;
  }

  public void clear() {
    this.captain1 = null;
    this.captain2 = null;
    this.ready1 = false;
    this.ready2 = false;
    this.isReadyActive = false;
    this.isMatchWithCaptains = false;
    this.isCaptain1Turn = true;
  }

  public int getCaptainTeam(UUID playerUUID) {
    int teamId = -1;
    if (captain1 != null && captain1.equals(playerUUID)) {
      teamId = 1;
    } else if (captain2 != null && captain2.equals(playerUUID)) {
      teamId = 2;
    }
    return teamId;
  }

  public boolean isReady1() {
    return ready1;
  }

  public void setReady1(boolean ready1, Match match) {
    this.ready1 = ready1;
    if (match != null) {
      checkReady(match);
    }
  }

  public boolean isReady2() {
    return ready2;
  }

  public void setReady2(boolean ready2, Match match) {
    this.ready2 = ready2;
    if (match != null) {
      checkReady(match);
    }
  }

  public void setReadyActive(boolean isReadyActive) {
    this.isReadyActive = isReadyActive;
  }

  public boolean isReadyActive() {
    return isReadyActive;
  }

  private void checkReady(Match match) {
    if (ready1 && ready2) {
      match
          .needModule(StartMatchModule.class)
          .forceStartCountdown(Duration.ofSeconds(5), Duration.ZERO);
      isReadyActive = false;
      Utilities.cancelReadyReminder();
      ready1 = false;
      ready2 = false;
    }
  }

  public void resetReady() {
    isReadyActive = false;
    ready1 = false;
    ready2 = false;
  }

  public boolean isMatchWithCaptains() {
    return isMatchWithCaptains;
  }

  public void setMatchWithCaptains(boolean isMatchWithCaptains) {
    this.isMatchWithCaptains = isMatchWithCaptains;
  }

  public boolean isCaptain1Turn() {
    return isCaptain1Turn;
  }

  public void setCaptain1Turn(boolean isCaptain1Turn) {
    this.isCaptain1Turn = isCaptain1Turn;
  }

  public void toggleTurn() {
    this.isCaptain1Turn = !this.isCaptain1Turn;
  }

  public UUID getCurrentCaptain() {
    return isCaptain1Turn ? captain1 : captain2;
  }

  public boolean isCaptain(UUID playerUUID) {
    return playerUUID.equals(captain1) || playerUUID.equals(captain2);
  }

  public void substituteCaptain(UUID oldCaptain, UUID newCaptain) {
    if (captain1 != null && captain1.equals(oldCaptain)) {
      captain1 = newCaptain;
    } else if (captain2 != null && captain2.equals(oldCaptain)) {
      captain2 = newCaptain;
    }
  }
}
