package org.nicolie.towersforpgm.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.commandUtils.StatsConfig;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;

public class CancelMatchCommand implements CommandExecutor {
  private final StatsConfig statsConfig;

  public CancelMatchCommand() {
    this.statsConfig = new StatsConfig();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    statsConfig.toggleStats(sender);
    Match match = PGM.get().getMatchManager().getMatch(sender);
    match.sendWarning(Component.text(
        org.nicolie.towersforpgm.utils.LanguageManager.langMessage("system.matchCancelled")));
    if (match.isRunning()) {
      sender.getServer().dispatchCommand(sender.getServer().getConsoleSender(), "end");
    } else {
      sender.getServer().dispatchCommand(sender.getServer().getConsoleSender(), "cycle 5");
    }
    return true;
  }
}
