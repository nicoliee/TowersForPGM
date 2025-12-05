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
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public class RemoveCommand implements CommandExecutor, TabCompleter {
  private final Draft draft;
  private final Teams teams;
  private final AvailablePlayers availablePlayers;
  private final PicksGUI pickInventory;

  public RemoveCommand(
      Draft draft,
      Teams teams,
      Captains captains,
      AvailablePlayers availablePlayers,
      PicksGUI pickInventory) {
    this.draft = draft;
    this.teams = teams;
    this.availablePlayers = availablePlayers;
    this.pickInventory = pickInventory;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(LanguageManager.message("errors.noPlayer"));
      return true;
    }
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer((Player) sender);
    if (!Draft.isDraftActive()) {
      matchPlayer.sendWarning(Component.text(LanguageManager.message("draft.picks.noDraft")));
      return true;
    }
    if (args.length < 1) {
      matchPlayer.sendWarning(Component.text(LanguageManager.message("draft.remove.usage")));
      return true;
    }
    if (Queue.isRanked()) {
      matchPlayer.sendWarning(Component.text(LanguageManager.message("ranked.notAllowed")));
      return true;
    }
    String playerName = args[0];
    if (isInvalidPlayer(playerName, matchPlayer)) {
      return true;
    }
    availablePlayers.removePlayer(playerName);
    pickInventory.updateAllInventories();
    String message = LanguageManager.message("draft.picks.remove").replace("{player}", playerName);
    matchPlayer.getMatch().sendMessage(Component.text(message));
    matchPlayer.getMatch().playSound(Sounds.WARNING);
    if (availablePlayers.getAllAvailablePlayers().isEmpty()) {
      draft.endDraft();
    }
    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      // Obtener la lista de tablas desde TowersForPGM.getTables()
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

  private boolean isInvalidPlayer(String playerName, MatchPlayer sender) {
    if (!availablePlayers.getAllAvailablePlayers().contains(playerName)) {
      sendErrorMessage(sender, "draft.remove.notInDraft");
      return true;
    }

    if (teams.isPlayerInAnyTeam(playerName)) {
      sendErrorMessage(sender, "draft.captains.alreadyInTeam");
      return true;
    }

    return false;
  }

  private void sendErrorMessage(MatchPlayer player, String messageKey) {
    player.sendWarning(Component.text(LanguageManager.message(messageKey)));
  }
}
