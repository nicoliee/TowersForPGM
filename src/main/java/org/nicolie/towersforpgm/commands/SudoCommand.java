package org.nicolie.towersforpgm.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

public class SudoCommand implements CommandExecutor {

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length < 2) {
      if (sender instanceof Player) {
        MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer((Player) sender);
        matchPlayer.sendWarning(Component.text(LanguageManager.message("system.sudo.usage")));
      } else {
        SendMessage.sendToConsole(LanguageManager.message("system.sudo.usage"));
      }
      return true;
    }

    String targetPlayerName = args[0];

    StringBuilder messageBuilder = new StringBuilder();
    for (int i = 1; i < args.length; i++) {
      messageBuilder.append(args[i]);
      if (i < args.length - 1) {
        messageBuilder.append(" ");
      }
    }
    String message = messageBuilder.toString();

    Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);

    if (targetPlayer == null) {
      if (sender instanceof Player) {
        MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer((Player) sender);
        matchPlayer.sendWarning(Component.text(LanguageManager.message("system.sudo.playerOffline")
            .replace("{player}", "§3" + targetPlayerName)));
      } else {
        SendMessage.sendToConsole(LanguageManager.message("system.sudo.playerOffline")
            .replace("{player}", "§3" + targetPlayerName));
      }
      return true;
    }

    if (message.startsWith("/")) {
      String commandToExecute = message.substring(1);
      Bukkit.dispatchCommand(targetPlayer, commandToExecute);
    } else {
      targetPlayer.chat(message);
    }

    return true;
  }
}
