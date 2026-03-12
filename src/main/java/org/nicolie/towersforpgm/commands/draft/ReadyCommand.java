package org.nicolie.towersforpgm.commands.draft;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Teams;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.util.Audience;

public class ReadyCommand implements CommandExecutor {
  private final Captains captains;
  private final Teams teams;

  public ReadyCommand(Captains captains, Teams teams) {
    this.captains = captains;
    this.teams = teams;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Audience audience = Audience.get(sender);
    if (!(sender instanceof Player)) {
      audience.sendWarning(Component.translatable("command.onlyPlayers"));
      return true;
    }

    Player player = (Player) sender;
    // Comprobar si el draft está activo
    if (!captains.isReadyActive()) {
      audience.sendWarning(Component.translatable("draft.ready.notAvailable"));
      return true;
    }
    int captainNumber = captains.getCaptainTeam(player.getUniqueId());
    if (captainNumber != -1) {
      boolean alreadyReady = captainNumber == 1 ? captains.isReady1() : captains.isReady2();

      if (alreadyReady) {
        audience.sendWarning(Component.translatable("draft.ready.alreadyReady"));
        return true;
      }
      if (captainNumber == 1) {
        captains.setReady1(true, PGM.get().getMatchManager().getMatch(player));
      } else {
        captains.setReady2(true, PGM.get().getMatchManager().getMatch(player));
      }
      Party team = teams.getTeam(captainNumber);
      Match match = PGM.get().getMatchManager().getMatch(player);
      match.sendMessage(Component.translatable("draft.ready", team.getName()));
    } else {
      audience.sendWarning(Component.translatable("draft.notCaptain"));
      return true;
    }
    return true;
  }
}
