package org.nicolie.towersforpgm.commands.ranked;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.DiscordManager;
import org.nicolie.towersforpgm.utils.LanguageManager;

public class UnlinkCommand implements CommandExecutor {
  private final TowersForPGM plugin = TowersForPGM.getInstance();

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission("towers.admin")) {
      sender.sendMessage(LanguageManager.message("errors.noPermission"));
      return true;
    }

    if (!plugin.getIsDatabaseActivated()) {
      sender.sendMessage(LanguageManager.message("matchbot.unlink.database-disabled"));
      return true;
    }

    if (args.length < 1) {
      sender.sendMessage(LanguageManager.message("matchbot.unlink.usage"));
      return true;
    }

    String identifier = args[0];
    sender.sendMessage(LanguageManager.message("matchbot.unlink.processing"));

    DiscordManager.unlinkPlayer(identifier)
        .thenAccept(success -> {
          if (success) {
            sender.sendMessage(LanguageManager.message("matchbot.unlink.success")
                .replace("{identifier}", identifier));
          } else {
            sender.sendMessage(LanguageManager.message("matchbot.unlink.notFound")
                .replace("{identifier}", identifier));
          }
        })
        .exceptionally(e -> {
          sender.sendMessage(LanguageManager.message("matchbot.unlink.error"));
          plugin.getLogger().severe("Error unlinking player: " + e.getMessage());
          return null;
        });

    return true;
  }
}
