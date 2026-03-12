package org.nicolie.towersforpgm.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.commands.commandUtils.StatsConfig;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.Audience;

public class CancelMatchCommand implements CommandExecutor {
  private final StatsConfig statsConfig;

  public CancelMatchCommand() {
    this.statsConfig = new StatsConfig();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Audience audience = Audience.get(sender);
    statsConfig.enabled(audience, true);
    Match match = PGM.get().getMatchManager().getMatch(sender);
    if (match.isRunning()) {
      sender.getServer().dispatchCommand(sender.getServer().getConsoleSender(), "end");
      match.sendWarning(Component.translatable("system.matchCancelled"));
    } else {
      audience.sendWarning(Component.translatable("command.matchNotStarted"));
    }
    return true;
  }
}
