package org.nicolie.towersforpgm.database.models.discordPlayer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class DiscordPlayerCache {
  private static final List<DiscordPlayer> players = new CopyOnWriteArrayList<>();

  public static DiscordPlayer getByUuid(UUID playerUuid) {
    return players.stream()
        .filter(player -> player.getPlayerUuid().equals(playerUuid))
        .findFirst()
        .orElse(null);
  }

  public static DiscordPlayer getByDiscordId(String discordId) {
    return players.stream()
        .filter(player -> player.getDiscordId().equals(discordId))
        .findFirst()
        .orElse(null);
  }

  public static void add(DiscordPlayer player) {
    if (player != null) {
      remove(player.getPlayerUuid(), player.getDiscordId());
      players.add(player);
    }
  }

  public static void remove(UUID playerUuid, String discordId) {
    players.removeIf(player -> (playerUuid != null && player.getPlayerUuid().equals(playerUuid))
        || (discordId != null && player.getDiscordId().equals(discordId)));
  }

  public static void clear() {
    players.clear();
  }
}
