package org.nicolie.towersforpgm.rankeds;

public enum Rank {
    BRONZE("Bronce", "§7", -100, -1),
    BRONZE_PLUS("Bronce+", "§7", 0, 99),
    SILVER("Plata", "§8", 100, 199),
    SILVER_PLUS("Plata+", "§8", 200, 299),
    GOLD("Oro", "§6", 300, 399),
    GOLD_PLUS("Oro+", "§6", 400, 499),
    EMERALD("Esmeralda", "§2", 500, 599),
    EMERALD_PLUS("Esmeralda+", "§2", 600, 699),
    DIAMOND("Diamante", "§9", 700, Integer.MAX_VALUE);

    private final String name;
    private final String color;
    private final int minElo;
    private final int maxElo;

    Rank(String name, String color, int minElo, int maxElo) {
        this.name = name;
        this.color = color;
        this.minElo = minElo;
        this.maxElo = maxElo;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public int getMinElo() {
        return minElo;
    }

    public int getMaxElo() {
        return maxElo;
    }

    public static Rank getRankByElo(int elo) {
        for (Rank rank : values()) {
            if (elo >= rank.getMinElo() && elo <= rank.getMaxElo()) {
                return rank;
            }
        }
        return BRONZE;
    }
}
