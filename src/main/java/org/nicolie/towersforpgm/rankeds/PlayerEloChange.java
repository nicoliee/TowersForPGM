package org.nicolie.towersforpgm.rankeds;

public class PlayerEloChange {
    private final String username;
    private final int currentElo;
    private final int newElo;
    private final int maxElo;

    public PlayerEloChange(String username, int currentElo, int newElo, int maxElo) {
        this.username = username;
        this.currentElo = currentElo;
        this.newElo = newElo;
        this.maxElo = maxElo;
    }

    public String getUsername() {
        return username;
    }

    public int getCurrentElo() {
        return currentElo;
    }

    public int getNewElo() {
        return newElo;
    }

    public int getMaxElo() {
        return maxElo;
    }

    @Override
    public String toString() {
        return "Username: " + username + ", CurrentElo: " + currentElo + ", NewElo: " + newElo + ", MaxElo: " + maxElo;
    }
}
