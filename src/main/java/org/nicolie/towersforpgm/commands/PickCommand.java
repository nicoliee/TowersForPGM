package org.nicolie.towersforpgm.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.PicksGUI;
import org.nicolie.towersforpgm.draft.Teams;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

// Comando para seleccionar un jugador en el draft
// PGM actualmente solo soporta una partida a la vez, por lo que no se pueden realizar múltiples
// drafts simultáneamente
public class PickCommand implements CommandExecutor, TabCompleter {
  private Draft draft;
  private final Captains captains;
  private final AvailablePlayers availablePlayers;
  private final Teams teams;
  private final PicksGUI pickInventory;

  public PickCommand(
      Draft draft,
      Captains captains,
      AvailablePlayers availablePlayers,
      Teams teams,
      PicksGUI pickInventory) {
    this.draft = draft;
    this.captains = captains;
    this.availablePlayers = availablePlayers;
    this.teams = teams;
    this.pickInventory = pickInventory;
  }

  private String validatePlayerToPick(String inputName, Player player) {
    if (teams.isPlayerInAnyTeam(inputName)) {
      return LanguageManager.message("draft.picks.alreadyPicked").replace("{player}", inputName);
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
      return LanguageManager.message("draft.picks.notInList").replace("{player}", inputName);
    }

    return null;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // Comprobar si el comando fue ejecutado por un jugador
    if (!(sender instanceof Player)) {
      sender.sendMessage(LanguageManager.message("errors.noPlayer"));
      return true;
    }

    Player player = (Player) sender;
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    // Comprobar si el draft está activo
    if (!Draft.isDraftActive()) {
      matchPlayer.sendWarning(Component.text(LanguageManager.message("draft.picks.noDraft")));
      return true;
    }

    if (args.length == 0) {
      pickInventory.openInventory(player);
      pickInventory.giveItemToPlayer(player);
      return true;
    }

    // Comprobar si el jugador es un capitán
    int captainNumber = captains.getCaptainTeam(player.getUniqueId());
    if (captainNumber != -1) {
      if (args.length == 1) {
        if ((captains.isCaptain1Turn() && captainNumber == 2)
            || (!captains.isCaptain1Turn() && captainNumber == 1)) {
          matchPlayer.sendWarning(Component.text(LanguageManager.message("draft.picks.notTurn")));
          return true;
        }

        String inputName = args[0].toLowerCase();
        String validationError = validatePlayerToPick(inputName, player);
        if (validationError != null) {
          matchPlayer.sendWarning(Component.text(validationError));
          return true;
        }

        draft.pickPlayer(inputName);
      }
    } else {
      SendMessage.sendToPlayer(player, LanguageManager.message("draft.picks.notCaptain"));
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
