package org.nicolie.towersforpgm.database;

public class Stats {
    private String username;
    private int kills;
    private int deaths;
    private int assists;
    private double damageDone;
    private double damageTaken;
    private int points;
    private int wins;
    private int games;
    private int winstreak;

    // Constructor
    public Stats(String username, int kills, int deaths, int assists, double damageDone, double damageTaken, int points, int wins, int games, int winstreak) {
        this.username = username;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.damageDone = damageDone;
        this.damageTaken = damageTaken;
        this.points = points;
        this.wins = wins;
        this.games = games;
        this.winstreak = winstreak;
    }

    @Override
    public String toString() {
        return "PlayerStatUpdate{" +
                "username='" + username + '\'' +
                ", kills=" + kills +
                ", deaths=" + deaths +
                ", assists=" + assists +
                ", damageDone=" + damageDone +
                ", damageTaken=" + damageTaken +
                ", points=" + points +
                ", wins=" + wins +
                ", games=" + games +
                ", winstreak=" + winstreak +
                '}';
    }

    public String getUsername() {return username;}
    public int getKills() {return kills;}
    public int getDeaths() {return deaths;}
    public int getAssists() {return assists;}
    public double getDamageDone() {return damageDone;}
    public double getDamageTaken() {return damageTaken;}
    public int getPoints() {return points;}
    public int getWins() {return wins;}
    public int getGames() {return games;}
    public int getWinstreak() {return winstreak;}
}