package org.nicolie.towersforpgm.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.rankeds.Queue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class CommandListener implements Listener {
  private final Captains captains;

  public CommandListener(Captains captains) {
    this.captains = captains;
  }

  // TODO: Messages in config
  @EventHandler
  public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
    String command = event.getMessage();
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());
    Match match = player.getMatch();
    Boolean ranked = Queue.isRanked();
    Boolean isCaptains = captains.isMatchWithCaptains();

    if (command.toLowerCase().startsWith("/end") && (ranked || isCaptains) && match.isRunning()) {
      if (command.toLowerCase().endsWith(" -f")) {
        String commandWithoutFlag = command.substring(0, command.length() - 3);
        event.setMessage(commandWithoutFlag);
      } else {
        event.setCancelled(true);
        String context = ranked ? " en una ranked." : " en un draft.";
        player.sendWarning(Component.text("No puedes hacer esto" + context));
      }
    } else if (command.toLowerCase().toLowerCase().equals("/team") && ranked) {
      if (command.endsWith(" -f")) {
        String commandWithoutFlag = command.replaceAll(" -f", "");
        event.setMessage(commandWithoutFlag);
      } else {
        event.setCancelled(true);
        player.sendWarning(Component.text("No puedes hacer eso en una ranked."));
      }
    }
  }
}
