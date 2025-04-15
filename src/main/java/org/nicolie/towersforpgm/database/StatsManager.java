package org.nicolie.towersforpgm.database;

import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

public class StatsManager {
    public static void updateStats(String table, List<Stats> playerStatsList) {
        if ("none".equalsIgnoreCase(table)) {
            return;
        }
        if (playerStatsList.isEmpty()) {
            return;
        }
    
        String sql = "INSERT INTO " + table + " (username, kills, deaths, assists, points, wins, games) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "kills = kills + VALUES(kills), " +
                     "deaths = deaths + VALUES(deaths), " +
                     "assists = assists + VALUES(assists), " +
                     "points = points + VALUES(points), " +
                     "wins = wins + VALUES(wins), " +
                     "games = games + VALUES(games)";
    
        Bukkit.getScheduler().runTaskAsynchronously(TowersForPGM.getInstance(), () -> {
            try (Connection conn = TowersForPGM.getInstance().getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
    
                conn.setAutoCommit(false);  // Deshabilita autocommit para mejorar rendimiento
    
                int batchSize = 0;
                for (Stats playerStat : playerStatsList) {
                    stmt.setString(1, playerStat.getUsername());
                    stmt.setInt(2, playerStat.getKills());
                    stmt.setInt(3, playerStat.getDeaths());
                    stmt.setInt(4, playerStat.getAssists());  // Asistencias
                    stmt.setInt(5, playerStat.getPoints());
                    stmt.setInt(6, playerStat.getWins());
                    stmt.setInt(7, playerStat.getGames());
    
                    stmt.addBatch();
                    batchSize++;
    
                    // Ejecuta en lotes de 100 registros para evitar consultas gigantes
                    if (batchSize % 100 == 0) {
                        stmt.executeBatch();
                    }
                }
    
                stmt.executeBatch();  // Ejecuta los datos restantes
                conn.commit();  // Confirma los cambios
    
            } catch (SQLException e) {
                TowersForPGM.getInstance().getLogger().log(Level.SEVERE, "Error al actualizar estadísticas", e);
                SendMessage.sendToDevelopers("§cError al actualizar estadísticas en la base de datos.");
            }
        });
    }    

    public static void showStats(CommandSender sender, String table, String player, LanguageManager languageManager) {
        if ("none".equalsIgnoreCase(table)) {
            return;
        }
        String sql = "SELECT kills, deaths, assists, points, wins, games FROM " + table + " WHERE username = ?";
        
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
                                + " §7| " + languageManager.getPluginMessage("stats.deaths") + ": §a" + rs.getInt("deaths")
                                + " §7| " + languageManager.getPluginMessage("stats.assists") + ": §a" + rs.getInt("assists")
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
        if ("none".equalsIgnoreCase(table)) {
            return;
        }
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
}