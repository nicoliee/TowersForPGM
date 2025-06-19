package org.nicolie.towersforpgm.rankeds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.utils.ConfigManager;

import tc.oc.pgm.api.player.MatchPlayer;

public class Elo {
    private static final int K_FACTOR = 32;

    public static List<PlayerEloChange> addWin(List<MatchPlayer> winners, List<MatchPlayer> losers) {
        List<PlayerEloChange> changes = new ArrayList<>();
        List<MatchPlayer> allPlayers = new ArrayList<>();
        allPlayers.addAll(winners);
        allPlayers.addAll(losers);
        List<String> usernames = allPlayers.stream().map(MatchPlayer::getNameLegacy).collect(Collectors.toList());
        String table = ConfigManager.getRankedDefaultTable();
        System.out.println("[Elo.addWin] Table: " + table);
        System.out.println("[Elo.addWin] Usernames: " + usernames);
        if (table == null || table.isEmpty()) {
            org.bukkit.Bukkit.getLogger().warning("[Elo.addWin] Table is null or empty");
            return null;
        }
        StatsManager.getEloForUsernames(table, usernames, eloList -> {
            System.out.println("[Elo.addWin] eloList: " + eloList);
            java.util.Map<String, PlayerEloChange> eloMap = eloList.stream().collect(Collectors.toMap(PlayerEloChange::getUsername, e -> e));
            java.util.Map<String, Integer> currentEloMap = eloMap.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getCurrentElo()));
            System.out.println("[Elo.addWin] currentEloMap: " + currentEloMap);
            double expectedScore = calculateExpectedScore(winners, losers, currentEloMap);
            System.out.println("[Elo.addWin] expectedScore: " + expectedScore);
            int eloChange = calculateEloChange(1.0, expectedScore);
            System.out.println("[Elo.addWin] eloChange: " + eloChange);

            winners.forEach(winner -> {
                String username = winner.getNameLegacy();
                PlayerEloChange playerElo = eloMap.getOrDefault(username, new PlayerEloChange(username, 0, 0, 0));
                int currentElo = playerElo.getCurrentElo();
                int newElo = currentElo + eloChange;
                int maxElo = playerElo.getMaxElo();
                if (newElo > maxElo) {
                    maxElo = newElo;
                }
                System.out.println("[Elo.addWin] WINNER " + username + " | currentElo: " + currentElo + ", newElo: " + newElo + ", maxElo: " + maxElo);
                changes.add(new PlayerEloChange(username, currentElo, newElo, maxElo));
            });

            losers.forEach(loser -> {
                String username = loser.getNameLegacy();
                PlayerEloChange playerElo = eloMap.getOrDefault(username, new PlayerEloChange(username, 0, 0, 0));
                int currentElo = playerElo.getCurrentElo();
                int newElo = currentElo - eloChange;
                if (newElo < -100) {
                    newElo = -100;
                }
                int maxElo = playerElo.getMaxElo();
                System.out.println("[Elo.addWin] LOSER " + username + " | currentElo: " + currentElo + ", newElo: " + newElo + ", maxElo: " + maxElo);
                changes.add(new PlayerEloChange(username, currentElo, newElo, maxElo));
            });
            System.out.println("[Elo.addWin] changes: " + changes);
        });
        return changes;
    }

    public static void eloOrder(List<MatchPlayer> players, java.util.function.BiConsumer<List<PlayerEloChange>, Integer> callback) {
        List<String> usernames = players.stream().map(MatchPlayer::getNameLegacy).collect(Collectors.toList());
        String table = ConfigManager.getRankedDefaultTable();
        System.out.println("[Elo.eloOrder] Table: " + table);
        System.out.println("[Elo.eloOrder] Usernames: " + usernames);
        if (table == null || table.isEmpty()) {
            org.bukkit.Bukkit.getLogger().warning("[Elo.eloOrder] Table is null or empty");
            callback.accept(null, 0);
            return;
        }
        StatsManager.getEloForUsernames(table, usernames, eloList -> {
            System.out.println("[Elo.eloOrder] eloList: " + eloList);
            List<PlayerEloChange> sortedList = eloList.stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getCurrentElo(), e1.getCurrentElo()))
                    .collect(Collectors.toList());
            int avgElo = 0;
            if (!sortedList.isEmpty()) {
                int total = sortedList.stream().mapToInt(PlayerEloChange::getCurrentElo).sum();
                avgElo = total / sortedList.size();
            }
            callback.accept(sortedList, avgElo);
        });
    }

    private static double calculateTeamAverageElo(List<MatchPlayer> team, Map<String, Integer> eloMap) {
        if (team.isEmpty()) return 0.0;

        double totalElo = 0.0;
        for (MatchPlayer player : team) {
            totalElo += eloMap.getOrDefault(player.getNameLegacy(), 0);
        }
        return totalElo / team.size();
    }

    private static double calculateExpectedScore(List<MatchPlayer> team1, List<MatchPlayer> team2, Map<String, Integer> eloMap) {
        double team1AverageElo = calculateTeamAverageElo(team1, eloMap);
        double team2AverageElo = calculateTeamAverageElo(team2, eloMap);
        return 1.0 / (1.0 + Math.pow(10.0, (team2AverageElo - team1AverageElo) / 400.0));
    }

    private static int calculateEloChange(double actualScore, double expectedScore) {
        return (int) Math.round(K_FACTOR * (actualScore - expectedScore));
    }

}
