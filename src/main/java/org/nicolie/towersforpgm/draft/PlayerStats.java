package org.nicolie.towersforpgm.draft;

public class PlayerStats {
    private int kills;
    private int deaths;
    private int assists;
    private int points;
    private int wins;
    private int games;

    public PlayerStats(int kills, int deaths, int assists, int points, int wins, int games) {
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.points = points;
        this.wins = wins;
        this.games = games;
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
