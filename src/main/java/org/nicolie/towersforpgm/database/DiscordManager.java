package org.nicolie.towersforpgm.database;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.models.discordPlayer.DiscordPlayer;
import org.nicolie.towersforpgm.database.models.discordPlayer.DiscordPlayerCache;
import org.nicolie.towersforpgm.database.sql.SQLDiscordManager;
import org.nicolie.towersforpgm.database.sqlite.SQLITEDiscordManager;

public class DiscordManager {

  private static final ExecutorService DISCORD_EXECUTOR = Executors.newFixedThreadPool(4, r -> {
    Thread t = new Thread(r);
    t.setName("TowersForPGM-Discord-Async");
    t.setDaemon(true);
    return t;
  });

  public static void shutdownDiscordExecutor() {
    DISCORD_EXECUTOR.shutdown();
    DiscordPlayerCache.clear();
  }

  public static CompletableFuture<Boolean> registerDCAccount(
      java.util.UUID playerUuid, String discordId) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (!plugin.getIsDatabaseActivated()) {
      return CompletableFuture.completedFuture(false);
    }

    String dbType = plugin.getCurrentDatabaseType();
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            boolean success = false;
            if ("MySQL".equals(dbType)) {
              success =
                  SQLDiscordManager.registerDCAccount(playerUuid, discordId).join();
            } else if ("SQLite".equals(dbType)) {
              success =
                  SQLITEDiscordManager.registerDCAccount(playerUuid, discordId).join();
            }

            // Si el registro fue exitoso, almacenar en caché
            if (success) {
              DiscordPlayerCache.add(new DiscordPlayer(playerUuid, discordId));
            }

            return success;
          } catch (Exception e) {
            plugin
                .getLogger()
                .severe("[DiscordManager] Error en registerDCAccount: " + e.getMessage());
            return false;
          }
        },
        DISCORD_EXECUTOR);
  }

  public static CompletableFuture<DiscordPlayer> getDiscordPlayer(java.util.UUID playerUuid) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (!plugin.getIsDatabaseActivated()) {
      return CompletableFuture.completedFuture(null);
    }

    DiscordPlayer cached = DiscordPlayerCache.getByUuid(playerUuid);
    if (cached != null) {
      return CompletableFuture.completedFuture(cached);
    }

    String dbType = plugin.getCurrentDatabaseType();
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            DiscordPlayer discordPlayer = null;
            if ("MySQL".equals(dbType)) {
              discordPlayer = SQLDiscordManager.getDiscordId(playerUuid).join();
            } else if ("SQLite".equals(dbType)) {
              discordPlayer = SQLITEDiscordManager.getDiscordId(playerUuid).join();
            }

            if (discordPlayer != null) {
              DiscordPlayerCache.add(discordPlayer);
            }

            return discordPlayer;
          } catch (Exception e) {
            plugin
                .getLogger()
                .severe("[DiscordManager] Error en getDiscordPlayer: " + e.getMessage());
            return null;
          }
        },
        DISCORD_EXECUTOR);
  }

  public static CompletableFuture<DiscordPlayer> getDiscordPlayer(String discordId) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    if (!plugin.getIsDatabaseActivated()) {
      return CompletableFuture.completedFuture(null);
    }

    DiscordPlayer cached = DiscordPlayerCache.getByDiscordId(discordId);
    if (cached != null) {
      return CompletableFuture.completedFuture(cached);
    }

    String dbType = plugin.getCurrentDatabaseType();
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            DiscordPlayer discordPlayer = null;
            if ("MySQL".equals(dbType)) {
              discordPlayer = SQLDiscordManager.getMinecraftUuid(discordId).join();
            } else if ("SQLite".equals(dbType)) {
              discordPlayer = SQLITEDiscordManager.getMinecraftUuid(discordId).join();
            }

            if (discordPlayer != null) {
              DiscordPlayerCache.add(discordPlayer);
            }

            return discordPlayer;
          } catch (Exception e) {
            plugin
                .getLogger()
                .severe("[DiscordManager] Error en getDiscordPlayer: " + e.getMessage());
            return null;
          }
        },
        DISCORD_EXECUTOR);
  }
}
