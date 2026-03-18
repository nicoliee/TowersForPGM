package org.nicolie.towersforpgm.commands.towers;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.MatchHistoryManager;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.*;
import tc.oc.pgm.util.Audience;

public class RollbackCommand {

  @Command("towers rollback <matchId>")
  @CommandDescription("Rollback a match by ID")
  public void rollback(
      Audience audience, CommandSender sender, @Argument("matchId") String matchId) {
    MatchHistoryManager.getMatch(matchId).thenAccept(history -> {
      if (history == null) {
        Bukkit.getScheduler().runTask(TowersForPGM.getInstance(), () -> {
          audience.sendMessage(
              Component.translatable("rollback.matchNotFound", Component.text(matchId)));
        });
        return;
      }
      MatchHistoryManager.rollbackMatch(audience, history);
    });
  }
}
