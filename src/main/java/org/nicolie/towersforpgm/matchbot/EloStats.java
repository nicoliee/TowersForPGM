package org.nicolie.towersforpgm.matchbot;

public class EloStats {
    private String username;
    private int kills;
    private int deaths;
    private int assists;
    private double damageDone;
    private double damageTaken;
    private double BowAccuracy;
    private int points;
    private int elo;
    private int eloChange;
    // Constructor
    public EloStats(String username, int kills, int deaths, int assists, double damageDone, double damageTaken, double BowAccuracy, int points, int elo, int eloChange) {
        this.username = username;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.damageDone = damageDone;
        this.damageTaken = damageTaken;
        this.BowAccuracy = BowAccuracy;
        this.points = points;
        this.elo = elo;
        this.eloChange = eloChange;
    }
    // Constructor retrocompatibilidad
    public EloStats(String username, int kills, int deaths, int assists, double damageDone, double damageTaken, double BowAccuracy, int points, int elo) {
        this(username, kills, deaths, assists, damageDone, damageTaken, BowAccuracy, points, elo, 0);
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
                ", BowAccuracy=" + BowAccuracy +
                ", points=" + points +
                ", elo=" + elo +
                ", eloChange=" + eloChange +
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

    public double getDamageDone() {
        return damageDone;
    }

    public double getDamageTaken() {
        return damageTaken;
    }

    public double getBowAccuracy() {
        return BowAccuracy;
    }

    public int getPoints() {
        return points;
    }

    public double getKDRatio() {
        if (deaths == 0) {
            return kills > 0 ? kills : 0.0;
        } else {
            return (double) kills / deaths;
        }
    }

    public int getElo() {
        return elo;
    }

    public int getEloChange() {
        return eloChange;
    }
}

