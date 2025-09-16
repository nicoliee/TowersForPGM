package org.nicolie.towersforpgm.database;

import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.rankeds.Rank;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

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
    public static void updateStats(String table, List<Stats> playerStatsList, List<PlayerEloChange> eloChange) {
        if (playerStatsList.isEmpty() || table == null || table.isEmpty() || (!ConfigManager.getTables().contains(table))) {
            return;
        }

        String sql = "INSERT INTO " + table + " (username, kills, deaths, assists, damageDone, damageTaken, points, wins, games, winstreak, maxWinstreak) " +
             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0) " +
             "ON DUPLICATE KEY UPDATE " +
             "kills = kills + VALUES(kills), " +
             "deaths = deaths + VALUES(deaths), " +
             "assists = assists + VALUES(assists), " +
             "damageDone = damageDone + VALUES(damageDone), " +
             "damageTaken = damageTaken + VALUES(damageTaken), " +
             "points = points + VALUES(points), " +
             "wins = wins + VALUES(wins), " +
             "games = games + VALUES(games), " +
             "winstreak = IF(VALUES(winstreak) = 1, winstreak + 1, 0), " +
             "maxWinstreak = GREATEST(maxWinstreak, winstreak)";


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
                    stmt.setInt(10, playerStat.getWinstreak());
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
                SendMessage.sendToDevelopers("§cError al actualizar estadísticas en la base de   datos.");
            }
        });
    }    

    public static void showStats(CommandSender sender, String table, String player, LanguageManager languageManager) {
        boolean isRankedTable = ConfigManager.getRankedTables().contains(table);
        String sql = isRankedTable
                ? "SELECT kills, deaths, assists, damageDone, damageTaken, points, wins, games, winstreak, maxWinstreak, elo, maxElo FROM " + table + " WHERE username = ?"
                : "SELECT kills, deaths, assists, damageDone, damageTaken, points, wins, winstreak, maxWinstreak, games FROM " + table + " WHERE username = ?";
        Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
            try (Connection conn = TowersForPGM.getInstance().getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        sender.sendMessage(languageManager.getPluginMessage("stats.header")
                                .replace("{player}", player)
                                .replace("{table}", table));
                        int games = rs.getInt("games");
                        double damageDone = games > 0 ? rs.getDouble("damageDone") / games : 0;
                        double damageTaken = games > 0 ? rs.getDouble("damageTaken") / games : 0;
                        if (isRankedTable) {
                            int elo = rs.getInt("elo");
                            int maxElo = rs.getInt("maxElo");
                            Rank rank = Rank.getRankByElo(elo);
                            Rank maxRank = Rank.getRankByElo(maxElo);
                            sender.sendMessage(Queue.RANKED_PREFIX + languageManager.getPluginMessage("stats.elo") + ": " + rank.getPrefixedRank(true)
                                    + " " + rank.getColor() + elo
                                    + " §7| " + languageManager.getPluginMessage("stats.maxElo") + ": " + rank.getPrefixedRank(true)
                                    + " " + maxRank.getColor() + maxElo);
                            sender.sendMessage(" ");
                        }
                        sender.sendMessage(" §7" + languageManager.getPluginMessage("stats.kills") + ": §a" + rs.getInt("kills")
                                + " §7| " + languageManager.getPluginMessage("stats.deaths") + ": §c" + rs.getInt("deaths")
                                + " §7| " + languageManager.getPluginMessage("stats.assists") + ": §a" + rs.getInt("assists")
                                + " §7| " + languageManager.getPluginMessage("stats.damageDone") + ": §a" + String.format("%.1f", damageDone)
                                + " §7| " + languageManager.getPluginMessage("stats.damageTaken") + ": §c" + String.format("%.1f", damageTaken)
                                + " §7| " + languageManager.getPluginMessage("stats.points") + ": §a" + rs.getInt("points")
                                + " §7| " + languageManager.getPluginMessage("stats.wins") + ": §a" + rs.getInt("wins")
                                + " §7| " + languageManager.getPluginMessage("stats.games") + ": §a" + games
                                + " §7| " + languageManager.getPluginMessage("stats.winstreak") + ": §a" + rs.getInt("winstreak")
                                + " §7| " + languageManager.getPluginMessage("stats.maxWinstreak") + ": §a" + rs.getInt("maxWinstreak"));
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
        boolean isRankedTable = ConfigManager.getRankedTables().contains(table);
        if ((category.equalsIgnoreCase("elo") || category.equalsIgnoreCase("maxelo")) && !isRankedTable) {
            sender.sendMessage("§cNo puedes ver el top de Elo en una tabla que no es ranked.");
            return;
        }
        String sql = "SELECT username, " + category + " FROM " + table + " ORDER BY " + category + " DESC LIMIT 10 OFFSET ?";

        Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
            try (Connection conn = TowersForPGM.getInstance().getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, offset);
                ResultSet rs = stmt.executeQuery();

                int rank = offset + 1;
                boolean hasResults = false;
                StringBuilder topList = new StringBuilder();

                while (rs.next()) {
                    if (category.equalsIgnoreCase("elo") || category.equalsIgnoreCase("maxElo")) {
                        int eloValue = rs.getInt(category);
                        Rank rankObj = Rank.getRankByElo(eloValue);
                        topList.append("§e#").append(rank).append(" ")
                              .append(rankObj.getPrefixedRank(true)).append(" §a")
                              .append(rs.getString("username")).append(" §7- ")
                              .append(rankObj.getColor()).append(eloValue).append("\n");
                    } else {
                        topList.append("§e#").append(rank).append(" §a")
                              .append(rs.getString("username")).append(" §7- ")
                              .append(rs.getInt(category)).append("\n");
                    }
                    rank++;
                    hasResults = true;
                }

                if (hasResults) {
                    SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("top.header")
                            .replace("{category}", category.toLowerCase())
                            .replace("{table}", table)
                            .replace("{page}", String.valueOf(page)));
                    sender.sendMessage(topList.toString().trim());
                } else {
                    sender.sendMessage("§7No hay más datos para mostrar.");
                }

            } catch (SQLException e) {
                TowersForPGM.getInstance().getLogger().log(Level.SEVERE, "Error al obtener el top de " + category + " en la tabla " + table, e);
                SendMessage.sendToDevelopers("§cError al obtener el top de " + category + " en la tabla " + table + " para el jugador " + sender.getName());
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
                java.util.Set<String> foundUsernames = new java.util.HashSet<>();
                while (rs.next()) {
                    String username = rs.getString("username");
                    int elo = rs.getInt("elo");
                    int maxElo = rs.getInt("maxElo");
                    result.add(new org.nicolie.towersforpgm.rankeds.PlayerEloChange(username, elo,0, 0, maxElo));
                    foundUsernames.add(username);
                }
                for (String username : usernames) {
                    if (!foundUsernames.contains(username)) {
                        result.add(new org.nicolie.towersforpgm.rankeds.PlayerEloChange(username, 0, 0, 0, 0));
                    }
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

    public static void getEloForUsername(String table, String username, Consumer<PlayerEloChange> callback) {
        if (username == null || username.isEmpty()) {
            callback.accept(new PlayerEloChange("", 0, 0, 0, 0));
            return;
        }
        String sql = "SELECT username, elo, maxElo FROM " + table + " WHERE username = ?";
        Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
            try (Connection conn = TowersForPGM.getInstance().getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String uname = rs.getString("username");
                    int elo = rs.getInt("elo");
                    int maxElo = rs.getInt("maxElo");
                    callback.accept(new PlayerEloChange(uname, elo, 0, 0, maxElo));
                } else {
                    callback.accept(null);
                }
            } catch (SQLException e) {
                TowersForPGM.getInstance().getLogger().log(Level.SEVERE, "Error al obtener el elo del usuario", e);
                callback.accept(null);
            }
        });
    }
}