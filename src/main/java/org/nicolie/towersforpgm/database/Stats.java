package org.nicolie.towersforpgm.database;

public class Stats {
    private String username;
    private int kills;
    private int deaths;
    private int assists;
    private int points;
    private int wins;
    private int games;

    // Constructor
    public Stats(String username, int kills, int deaths, int assists, int points, int wins, int games) {
        this.username = username;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.points = points;
        this.wins = wins;
        this.games = games;
    }

    @Override
    public String toString() {
        return "PlayerStatUpdate{" +
                "username='" + username + '\'' +
                ", kills=" + kills +
                ", deaths=" + deaths +
                ", assists=" + assists +
                ", points=" + points +
                ", wins=" + wins +
                ", games=" + games +
                '}';
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getAssists() {
        return assists;
    }

    public int getPoints() {
        return points;
    }

    public int getWins() {
        return wins;
    }

    public int getGames() {
        return games;
    }
}