package org.nicolie.towersforpgm.database;

import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class StatsManager {
    public static void updateStats(String table, List<Stats> playerStatsList) {
        updateStats(table, playerStatsList, null);
    }

    public static void updateStats(String table, List<Stats> playerStatsList, List<PlayerEloChange> eloChange) {
        if (playerStatsList.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO " + table + " (username, kills, deaths, assists, damageDone, damageTaken, points, wins, games) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "kills = VALUES(kills) + kills, " +
                     "deaths = VALUES(deaths) + deaths, " +
                     "assists = VALUES(assists) + assists, " +
                     "damageDone = VALUES(damageDone) + damageDone, " +
                     "damageTaken = VALUES(damageTaken) + damageTaken, " +
                     "points = VALUES(points) + points, " +
                     "wins = VALUES(wins) + wins, " +
                     "games = VALUES(games) + games";

        String rankedSql = "UPDATE " + table + " SET elo = ?, lastElo = ?, maxElo = ? WHERE username = ?";

        Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
            try (Connection conn = TowersForPGM.getInstance().getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                conn.setAutoCommit(false);  // Deshabilita autocommit para mejorar rendimiento

                int batchSize = 0;
                for (Stats playerStat : playerStatsList) {
                    stmt.setString(1, playerStat.getUsername());
                    stmt.setInt(2, playerStat.getKills());
                    stmt.setInt(3, playerStat.getDeaths());
                    stmt.setInt(4, playerStat.getAssists());
                    stmt.setDouble(5, playerStat.getDamageDone());
                    stmt.setDouble(6, playerStat.getDamageTaken());
                    stmt.setInt(7, playerStat.getPoints());
                    stmt.setInt(8, playerStat.getWins());
                    stmt.setInt(9, playerStat.getGames());
                    stmt.addBatch();
                    batchSize++;
                    if (batchSize % 100 == 0) {
                        stmt.executeBatch();
                    }
                }
                stmt.executeBatch();  // Ejecuta los datos restantes
                conn.commit();  // Confirma los cambios

                // Si se recibe un PlayerEloChange, actualiza elo, lastElo y maxElo en la base de datos
                if (eloChange != null && !eloChange.isEmpty()) {
                    try (PreparedStatement rankedStmt = conn.prepareStatement(rankedSql)) {
                        for (PlayerEloChange change : eloChange) {
                            rankedStmt.setInt(1, change.getNewElo());
                            rankedStmt.setInt(2, change.getCurrentElo());
                            rankedStmt.setInt(3, change.getMaxElo());
                            rankedStmt.setString(4, change.getUsername());
                            rankedStmt.addBatch();
                        }
                        rankedStmt.executeBatch();
                        conn.commit();
                    }
                }

            } catch (SQLException e) {
                TowersForPGM.getInstance().getLogger().log(Level.SEVERE, "Error al actualizar estadísticas", e);
                SendMessage.sendToDevelopers("§cError al actualizar estadísticas en la base de datos.");
            }
        });
    }    

    public static void showStats(CommandSender sender, String table, String player, LanguageManager languageManager) {
        String sql = "SELECT kills, deaths, assists, damageDone, damageTaken, points, wins, games FROM " + table + " WHERE username = ?";
        
        Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
            try (Connection conn = TowersForPGM.getInstance().getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                 
                stmt.setString(1, player);
                
                try (ResultSet rs = stmt.executeQuery()) { // ResultSet dentro del try
                    if (rs.next()) {
                        sender.sendMessage(languageManager.getPluginMessage("stats.header")
                                .replace("{player}", player)
                                .replace("{table}", table));
                        sender.sendMessage("  §7" + languageManager.getPluginMessage("stats.kills") + ": §a" + rs.getInt("kills")
                                + " §7| " + languageManager.getPluginMessage("stats.deaths") + ": §c" + rs.getInt("deaths")
                                + " §7| " + languageManager.getPluginMessage("stats.assists") + ": §a" + rs.getInt("assists")
                                + " §7| " + languageManager.getPluginMessage("stats.damageDone") + ": §a" + String.format("%.1f", rs.getDouble("damageDone"))
                                + " §7| " + languageManager.getPluginMessage("stats.damageTaken") + ": §c" + String.format("%.1f", rs.getDouble("damageTaken"))
                                + " §7| " + languageManager.getPluginMessage("stats.points") + ": §a" + rs.getInt("points")
                                + " §7| " + languageManager.getPluginMessage("stats.wins") + ": §a" + rs.getInt("wins")
                                + " §7| " + languageManager.getPluginMessage("stats.games") + ": §a" + rs.getInt("games"));
                    } else {
                        sender.sendMessage(languageManager.getPluginMessage("stats.noStats"));
                    }
                }
                
            } catch (SQLException e) {
                sender.sendMessage(languageManager.getPluginMessage("stats.error"));
                SendMessage.sendToDevelopers("§cError al obtener las estadísticas de " + player + " en la tabla " + table);
                TowersForPGM.getInstance().getLogger().severe("Error SQL: " + e.getMessage());
            }
        });
    }    

    public static void showTop(String category, int page, String table, CommandSender sender, LanguageManager languageManager) {
        int offset = (page - 1) * 10;
        String sql = "SELECT username, " + category + " FROM " + table + " ORDER BY " + category + " DESC LIMIT 10 OFFSET ?";

        Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
            try (Connection conn = TowersForPGM.getInstance().getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, offset);
                ResultSet rs = stmt.executeQuery();
                Player player = (Player) sender;
                SendMessage.sendToPlayer(player, languageManager.getPluginMessage("top.header")
                        .replace("{category}", category.toLowerCase())
                        .replace("{table}", table)
                        .replace("{page}", String.valueOf(page)));

                int rank = offset + 1;
                boolean hasResults = false;
                while (rs.next()) {
                    sender.sendMessage("§e#" + rank + " §a" + rs.getString("username") + " §7- " + rs.getInt(category));
                    rank++;
                    hasResults = true;
                }

                if (!hasResults) {
                    sender.sendMessage("§7No hay más datos para mostrar.");
                }

            } catch (SQLException e) {
                TowersForPGM.getInstance().getLogger().log(Level.SEVERE, "Error al obtener el top de " + category + " en la tabla " + table, e);
                SendMessage.sendToDevelopers("§cError al obtener el top de " + category + " en la tabla " + table + "para el jugador " + sender.getName());
                sender.sendMessage("§cHubo un error al obtener los datos.");
            }
        });
    }

    public static void getEloForUsernames(String table, List<String> usernames, Consumer<List<PlayerEloChange>> callback) {
        if (usernames == null || usernames.isEmpty()) {
            callback.accept(Collections.emptyList());
            return;
        }

        String placeholders = usernames.stream().map(u -> "?").collect(Collectors.joining(","));
        String sql = "SELECT username, elo, maxElo FROM " + table + " WHERE username IN (" + placeholders + ")";

        Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
            try (Connection conn = TowersForPGM.getInstance().getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < usernames.size(); i++) {
                    stmt.setString(i + 1, usernames.get(i));
                }

                ResultSet rs = stmt.executeQuery();
                List<org.nicolie.towersforpgm.rankeds.PlayerEloChange> result = new java.util.ArrayList<>();
                while (rs.next()) {
                    String username = rs.getString("username");
                    int elo = rs.getInt("elo");
                    int maxElo = rs.getInt("maxElo");
                    result.add(new org.nicolie.towersforpgm.rankeds.PlayerEloChange(username, elo, elo, maxElo));
                }
                // Mantener el orden de entrada
                List<org.nicolie.towersforpgm.rankeds.PlayerEloChange> orderedResult = new java.util.ArrayList<>();
                for (String username : usernames) {
                    for (org.nicolie.towersforpgm.rankeds.PlayerEloChange change : result) {
                        if (change.getUsername().equals(username)) {
                            orderedResult.add(change);
                            break;
                        }
                    }
                }
                callback.accept(orderedResult);
            } catch (SQLException e) {
                TowersForPGM.getInstance().getLogger().log(Level.SEVERE, "Error al obtener el elo de los usuarios", e);
                callback.accept(Collections.emptyList());
            }
        });
    }
}