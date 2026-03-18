package org.nicolie.towersforpgm.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.commands.towers.commandUtils.StatsConfig;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.util.Audience;

public class CancelMatchCommand {
  private final StatsConfig statsConfig = new StatsConfig();

  @Command("cancelmatch")
  @CommandDescription("Cancel the current match")
  public void cancelMatch(Audience audience, CommandSender sender) {
    statsConfig.enabled(audience, true);
    Match match = PGM.get().getMatchManager().getMatch(sender);
    if (match.isRunning()) {
      sender.getServer().dispatchCommand(sender.getServer().getConsoleSender(), "end");
      match.sendWarning(Component.translatable("system.matchCancelled"));
    } else {
      audience.sendWarning(Component.translatable("command.matchNotStarted"));
    }
  }
}
