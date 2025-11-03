package org.nicolie.towersforpgm.database;

import java.util.concurrent.CompletableFuture;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.sql.SQLDiscordManager;
import org.nicolie.towersforpgm.database.sqlite.SQLITEDiscordManager;

public class DiscordManager {

  /**
   * Registra una nueva vinculación entre cuenta de Minecraft y Discord
   *
   * @param playerUuid UUID del jugador de Minecraft
   * @param discordId ID del usuario de Discord
   * @return CompletableFuture<Boolean> - true si se registró exitosamente
   */
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
        plugin.getLogger().warning("Tipo de base de datos desconocido: " + dbType);
        return CompletableFuture.completedFuture(false);
      }
    } catch (Exception e) {
      plugin.getLogger().severe("Error registrando cuenta Discord: " + e.getMessage());
      return CompletableFuture.completedFuture(false);
    }
  }

  /**
   * Busca el Discord ID asociado a un UUID de Minecraft
   *
   * @param playerUuid UUID del jugador
   * @return CompletableFuture<String> - Discord ID o null si no encontrado
   */
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
        plugin.getLogger().warning("Tipo de base de datos desconocido: " + dbType);
        return CompletableFuture.completedFuture(null);
      }
    } catch (Exception e) {
      plugin.getLogger().severe("Error obteniendo Discord ID: " + e.getMessage());
      return CompletableFuture.completedFuture(null);
    }
  }

  /**
   * Busca el UUID de Minecraft asociado a un Discord ID
   *
   * @param discordId ID del usuario de Discord
   * @return CompletableFuture<java.util.UUID> - UUID del jugador o null si no encontrado
   */
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
        plugin.getLogger().warning("Tipo de base de datos desconocido: " + dbType);
        return CompletableFuture.completedFuture(null);
      }
    } catch (Exception e) {
      plugin.getLogger().severe("Error obteniendo UUID de Minecraft: " + e.getMessage());
      return CompletableFuture.completedFuture(null);
    }
  }
}
