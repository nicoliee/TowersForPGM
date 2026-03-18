package org.nicolie.towersforpgm.commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.utils.Permissions;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.util.Audience;

public class SudoCommand {

  @Command("sudo <target> <message>")
  @CommandDescription("Execute a command or send a message as another player")
  @Permission(Permissions.DEVELOPER)
  public void executeSudo(
      Audience audience, @Argument("target") Player target, @Argument("message") String message) {
    if (message.startsWith("/")) {
      String commandToExecute = message.substring(1);
      Bukkit.dispatchCommand(target, commandToExecute);
    } else {
      target.chat(message);
    }
  }
}
