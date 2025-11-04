package org.nicolie.towersforpgm.database.models.discordPlayer;

import java.util.UUID;

public class DiscordPlayer {
  private final UUID playerUuid;
  private final String discordId;

  public DiscordPlayer(UUID playerUuid, String discordId) {
    this.playerUuid = playerUuid;
    this.discordId = discordId;
  }

  public UUID getPlayerUuid() {
    return playerUuid;
  }

  public String getDiscordId() {
    return discordId;
  }

  public UUID getUuidByDiscordId(String searchDiscordId) {
    return (discordId != null && discordId.equals(searchDiscordId)) ? playerUuid : null;
  }

  public String getDiscordIdByUuid(UUID searchUuid) {
    return (playerUuid != null && playerUuid.equals(searchUuid)) ? discordId : null;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    DiscordPlayer that = (DiscordPlayer) obj;
    return playerUuid.equals(that.playerUuid) && discordId.equals(that.discordId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(playerUuid, discordId);
  }

  @Override
  public String toString() {
    return "DiscordPlayer{" + "playerUuid=" + playerUuid + ", discordId='" + discordId + '\'' + '}';
  }
}
