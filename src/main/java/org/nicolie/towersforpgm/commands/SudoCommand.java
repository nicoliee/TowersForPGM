package org.nicolie.towersforpgm.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.Audience;

public class SudoCommand implements CommandExecutor {

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Audience audience = Audience.get(sender);
    if (args.length < 2) {
      audience.sendWarning(Component.translatable(
          "command.incorrectUsage", Component.text("/sudo <playerName> <command/message>")));
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
      audience.sendWarning(Component.translatable(
          "command.playerNotFound",
          Component.text(targetPlayerName).color(NamedTextColor.DARK_AQUA)));
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
