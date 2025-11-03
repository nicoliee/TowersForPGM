package org.nicolie.towersforpgm.database.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;

public class SQLDiscordManager {

  private static final ExecutorService SQL_EXECUTOR = Executors.newFixedThreadPool(4, r -> {
    Thread t = new Thread(r);
    t.setName("TowersForPGM-SQL-Discord");
    t.setDaemon(true);
    return t;
  });

  public static void shutdownSqlExecutor() {
    SQL_EXECUTOR.shutdown();
  }

  public static CompletableFuture<Boolean> registerDCAccount(UUID playerUuid, String discordId) {
    return CompletableFuture.supplyAsync(
        () -> {
          String acc_table = MatchBotConfig.getAccountsTable();
          try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection()) {

            // Verificar si ya existe el UUID o Discord ID
            String checkSQL =
                "SELECT COUNT(*) FROM " + acc_table + " WHERE uuid = ? OR discordId = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSQL)) {
              checkStmt.setString(1, playerUuid.toString());
              checkStmt.setString(2, discordId);

              try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                  TowersForPGM.getInstance()
                      .getLogger()
                      .warning("Intento de vinculación duplicada: UUID=" + playerUuid
                          + ", DiscordID=" + discordId);
                  return false;
                }
              }
            }

            // Insertar nueva vinculación
            String insertSQL = "INSERT INTO " + acc_table + " (uuid, discordId) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
              insertStmt.setString(1, playerUuid.toString());
              insertStmt.setString(2, discordId);

              int affected = insertStmt.executeUpdate();
              if (affected > 0) {
                TowersForPGM.getInstance()
                    .getLogger()
                    .info("Cuenta vinculada exitosamente: UUID=" + playerUuid + ", DiscordID="
                        + discordId);
                return true;
              }
            }

          } catch (SQLException e) {
            TowersForPGM.getInstance()
                .getLogger()
                .log(
                    Level.SEVERE,
                    "Error registrando cuenta: UUID=" + playerUuid + ", DiscordID=" + discordId,
                    e);
          }

          return false;
        },
        SQL_EXECUTOR);
  }

  public static CompletableFuture<String> getDiscordId(UUID playerUuid) {
    return CompletableFuture.supplyAsync(
        () -> {
          String acc_table = MatchBotConfig.getAccountsTable();
          try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection()) {
            String sql = "SELECT discordId FROM " + acc_table + " WHERE uuid = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
              stmt.setString(1, playerUuid.toString());

              try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                  return rs.getString("discordId");
                }
              }
            }
          } catch (SQLException e) {
            TowersForPGM.getInstance()
                .getLogger()
                .log(Level.SEVERE, "Error buscando Discord ID para UUID: " + playerUuid, e);
          }
          return null;
        },
        SQL_EXECUTOR);
  }

  public static CompletableFuture<UUID> getMinecraftUuid(String discordId) {
    return CompletableFuture.supplyAsync(
        () -> {
          String acc_table = MatchBotConfig.getAccountsTable();
          try (Connection conn = TowersForPGM.getInstance().getDatabaseConnection()) {
            String sql = "SELECT uuid FROM " + acc_table + " WHERE discordId = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
              stmt.setString(1, discordId);

              try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                  return java.util.UUID.fromString(rs.getString("uuid"));
                }
              }
            }

          } catch (SQLException e) {
            TowersForPGM.getInstance()
                .getLogger()
                .log(Level.SEVERE, "Error buscando UUID para Discord ID: " + discordId, e);
          }

          return null;
        },
        SQL_EXECUTOR);
  }
}
