package org.nicolie.towersforpgm.commands.ranked;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.DiscordManager;
import tc.oc.pgm.util.Audience;

public class UnlinkCommand implements CommandExecutor {
  private final TowersForPGM plugin = TowersForPGM.getInstance();

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Audience audience = Audience.get(sender);
    if (!sender.hasPermission("towers.admin")) {
      audience.sendMessage(Component.translatable("misc.noPermission"));
      return true;
    }

    if (args.length < 1) {
      audience.sendMessage(Component.translatable(
          "command.incorrectUsage", Component.text("/unlink <playerName|discordId>")));
      return true;
    }

    String identifier = args[0];
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

    return true;
  }

  private Component formatIdentifier(String identifier) {
    Component idComponent = Component.text("[")
        .color(NamedTextColor.DARK_GRAY)
        .append(Component.text(identifier)
            .color(NamedTextColor.GOLD)
            .append(Component.text("]").color(NamedTextColor.DARK_GRAY)));
    return idComponent;
  }
}
