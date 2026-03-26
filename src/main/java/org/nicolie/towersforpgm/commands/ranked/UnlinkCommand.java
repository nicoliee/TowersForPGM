package org.nicolie.towersforpgm.commands.ranked;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.DiscordManager;
import org.nicolie.towersforpgm.utils.Permissions;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.util.Audience;

public class UnlinkCommand {
  private final TowersForPGM plugin = TowersForPGM.getInstance();

  @Command("unlink discord <identifier>")
  @CommandDescription("Unlink a Discord account by Discord ID")
  @Permission(Permissions.ADMIN)
  public void unlinkCommand(Audience audience, @Argument("identifier") String identifier) {
    DiscordManager.unlinkPlayer(identifier)
        .thenAccept(success -> {
          if (success) {
            audience.sendMessage(
                Component.translatable("matchbot.unlink.success", formatIdentifier(identifier))
                    .color(NamedTextColor.GREEN));
          } else {
            audience.sendMessage(
                Component.translatable("matchbot.unlink.notFound", formatIdentifier(identifier))
                    .color(NamedTextColor.RED));
          }
        })
        .exceptionally(e -> {
          audience.sendMessage(Component.translatable("matchbot.unlink.error"));
          plugin.getLogger().severe("Error unlinking player: " + e.getMessage());
          return null;
        });
  }

  @Command("unlink player <player>")
  // @CommandDescription("Unlink a Discord account by player name")
  @Permission(Permissions.ADMIN)
  public void unlinkPlayerCommand(Audience audience, @Argument("player") OfflinePlayer player) {
    DiscordManager.unlinkPlayer(player.getUniqueId())
        .thenAccept(success -> {
          if (success) {
            audience.sendMessage(Component.translatable(
                    "matchbot.unlink.success", formatIdentifier(player.getName()))
                .color(NamedTextColor.GREEN));
          } else {
            audience.sendMessage(Component.translatable(
                    "matchbot.unlink.notFound", formatIdentifier(player.getName()))
                .color(NamedTextColor.RED));
          }
        })
        .exceptionally(e -> {
          audience.sendMessage(Component.translatable("matchbot.unlink.error"));
          plugin.getLogger().severe("Error unlinking player: " + e.getMessage());
          return null;
        });
  }

  private Component formatIdentifier(String identifier) {
    return Component.text("[")
        .color(NamedTextColor.DARK_GRAY)
        .append(Component.text(identifier)
            .color(NamedTextColor.GOLD)
            .append(Component.text("]").color(NamedTextColor.DARK_GRAY)));
  }
}
