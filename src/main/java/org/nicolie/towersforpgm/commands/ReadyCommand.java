package org.nicolie.towersforpgm.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

public class ReadyCommand implements CommandExecutor {
  private final Captains captains;

  public ReadyCommand(Captains captains) {
    this.captains = captains;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(LanguageManager.message("errors.noPlayer"));
      return true;
    }

    Player player = (Player) sender;
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    // Comprobar si el draft está activo
    if (!captains.isReadyActive()) {
      matchPlayer.sendWarning(Component.text(LanguageManager.message("draft.ready.notAvailable")));
      return true;
    }
    int captainNumber = captains.getCaptainTeam(player.getUniqueId());
    if (captainNumber != -1) {
      boolean alreadyReady = captainNumber == 1 ? captains.isReady1() : captains.isReady2();

      if (alreadyReady) {
        matchPlayer.sendWarning(
            Component.text(LanguageManager.message("draft.ready.alreadyReady")));
        return true;
      }
      if (captainNumber == 1) {
        captains.setReady1(true, PGM.get().getMatchManager().getMatch(player));
      } else {
        captains.setReady2(true, PGM.get().getMatchManager().getMatch(player));
      }
      matchPlayer.getMatch().sendMessage(Component.text(getReadyMessage(matchPlayer)));
    } else {
      matchPlayer.sendWarning(Component.text(LanguageManager.message("draft.picks.notCaptain")));
      return true;
    }
    return true;
  }

  public static String getReadyMessage(MatchPlayer matchPlayer) {
    String teamColor = matchPlayer.getParty().getColor().toString();
    String teamName = matchPlayer.getParty().getNameLegacy();
    return LanguageManager.message("draft.ready.ready")
        .replace("{teamcolor}", teamColor)
        .replace("{team}", teamName);
  }
}
