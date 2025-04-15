package org.nicolie.towersforpgm.draft;

import java.util.UUID;

import org.bukkit.Bukkit;

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

    public boolean isCaptain(UUID player) {
        return player.equals(captain1) || player.equals(captain2);
    }

    public boolean isCaptain1(UUID player) {
        return player.equals(captain1);
    }
    
    public boolean isCaptain2(UUID player) {
        return player.equals(captain2);
    }
    
    public boolean isReady1() {
        return ready1;
    }

    public void setReady1(boolean ready1) {
        this.ready1 = ready1;
        checkAndTriggerEvent();
    }

    public boolean isReady2() {
        return ready2;
    }

    public void setReady2(boolean ready2) {
        this.ready2 = ready2;
        checkAndTriggerEvent();
    }

    public void setReadyActive(boolean isReadyActive) {
        this.isReadyActive = isReadyActive;
    }
    
    public boolean isReadyActive() {
        return isReadyActive;
    }

    private void checkAndTriggerEvent() {
        if (ready1 && ready2) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "start 5");
        }
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
}