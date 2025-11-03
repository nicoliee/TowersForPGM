package org.nicolie.towersforpgm.database;

import java.util.concurrent.CompletableFuture;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.sql.SQLDiscordManager;
import org.nicolie.towersforpgm.database.sqlite.SQLITEDiscordManager;

public class DiscordManager {

  public static CompletableFuture<Boolean> registerDCAccount(
      java.util.UUID playerUuid, String discordId) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (!plugin.getIsDatabaseActivated()) {
      return CompletableFuture.completedFuture(false);
    }

    String dbType = plugin.getCurrentDatabaseType();
    try {
      if ("MySQL".equals(dbType)) {
        return SQLDiscordManager.registerDCAccount(playerUuid, discordId);
      } else if ("SQLite".equals(dbType)) {
        return SQLITEDiscordManager.registerDCAccount(playerUuid, discordId);
      } else {
        return CompletableFuture.completedFuture(false);
      }
    } catch (Exception e) {
      return CompletableFuture.completedFuture(false);
    }
  }

  public static CompletableFuture<String> getDiscordId(java.util.UUID playerUuid) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (!plugin.getIsDatabaseActivated()) {
      return CompletableFuture.completedFuture(null);
    }

    String dbType = plugin.getCurrentDatabaseType();
    try {
      if ("MySQL".equals(dbType)) {
        return SQLDiscordManager.getDiscordId(playerUuid);
      } else if ("SQLite".equals(dbType)) {
        return SQLITEDiscordManager.getDiscordId(playerUuid);
      } else {
        return CompletableFuture.completedFuture(null);
      }
    } catch (Exception e) {
      return CompletableFuture.completedFuture(null);
    }
  }

  public static CompletableFuture<java.util.UUID> getMinecraftUuid(String discordId) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (!plugin.getIsDatabaseActivated()) {
      return CompletableFuture.completedFuture(null);
    }

    String dbType = plugin.getCurrentDatabaseType();
    try {
      if ("MySQL".equals(dbType)) {
        return SQLDiscordManager.getMinecraftUuid(discordId);
      } else if ("SQLite".equals(dbType)) {
        return SQLITEDiscordManager.getMinecraftUuid(discordId);
      } else {
        return CompletableFuture.completedFuture(null);
      }
    } catch (Exception e) {
      return CompletableFuture.completedFuture(null);
    }
  }
}
