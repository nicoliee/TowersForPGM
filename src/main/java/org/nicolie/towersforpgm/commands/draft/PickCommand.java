package org.nicolie.towersforpgm.commands.draft;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.components.DraftPhase;
import org.nicolie.towersforpgm.draft.core.AvailablePlayers;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Draft;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.draft.picksMenu.PicksGUIManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.Audience;

public class PickCommand implements CommandExecutor, TabCompleter {
  private Draft draft;
  private final Captains captains;
  private final AvailablePlayers availablePlayers;
  private final Teams teams;

  public PickCommand(
      Draft draft, Captains captains, AvailablePlayers availablePlayers, Teams teams) {
    this.draft = draft;
    this.captains = captains;
    this.availablePlayers = availablePlayers;
    this.teams = teams;
  }

  private String validatePlayerToPick(String inputName, Player player) {
    if (teams.isPlayerInAnyTeam(inputName)) {
      return "draft.alreadyInTeam";
    }

    String pickedPlayerString = availablePlayers.getAvailablePlayers().stream()
        .map(MatchPlayer::getNameLegacy)
        .filter(name -> name.equalsIgnoreCase(inputName))
        .findFirst()
        .orElseGet(() -> availablePlayers.getAvailableOfflinePlayers().stream()
            .filter(name -> name.equalsIgnoreCase(inputName))
            .findFirst()
            .orElse(null));

    if (pickedPlayerString == null) {
      return "draft.picks.notInDraft";
    }

    return null;
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
    if (Draft.getPhase() == DraftPhase.IDLE || Draft.getPhase() == DraftPhase.ENDED) {
      audience.sendWarning(Component.translatable("draft.inactive"));
      return true;
    }

    // Prevenir picks durante fase de reroll de capitanes
    if (Draft.getPhase() == DraftPhase.CAPTAINS || Draft.getPhase() == DraftPhase.REROLL) {
      audience.sendWarning(Component.translatable("draft.picks.duringReroll"));
      return true;
    }

    if (args.length == 0) {
      MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
      new PicksGUIManager(TowersForPGM.getInstance(), draft, captains, availablePlayers, teams)
          .openMenu(matchPlayer);
      PicksGUIManager.giveItem(player);
      return true;
    }

    // Comprobar si el jugador es un capitán
    int captainNumber = captains.getCaptainTeam(player.getUniqueId());
    if (captainNumber != -1) {
      if (args.length == 1) {
        if ((captains.isCaptain1Turn() && captainNumber == 2)
            || (!captains.isCaptain1Turn() && captainNumber == 1)) {
          audience.sendWarning(Component.translatable("draft.notTurn"));
          return true;
        }

        String inputName = args[0].toLowerCase();
        String validationError = validatePlayerToPick(inputName, player);
        if (validationError != null) {
          audience.sendWarning(Component.translatable(validationError, Component.text(inputName)));
          return true;
        }

        draft.pickPlayer(inputName);
      }
    } else {
      audience.sendWarning(Component.translatable("draft.notCaptain"));
      return true;
    }
    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      List<String> tables = availablePlayers.getAvailablePlayers().stream()
          .map(MatchPlayer::getNameLegacy)
          .collect(Collectors.toList());
      tables.addAll(availablePlayers.getAvailableOfflinePlayers());
      // Filtrar las opciones que comienzan con el texto ingresado por el usuario
      String input = args[0].toLowerCase();
      List<String> filteredOptions = new ArrayList<>();
      for (String table : tables) {
        if (table.toLowerCase().startsWith(input)) {
          filteredOptions.add(table);
        }
      }
      return filteredOptions;
    }
    return null;
  }
}
