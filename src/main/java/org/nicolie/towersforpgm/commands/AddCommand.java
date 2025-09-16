package org.nicolie.towersforpgm.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.PickInventory;
import org.nicolie.towersforpgm.draft.Teams;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public class AddCommand implements CommandExecutor {
  private final AvailablePlayers availablePlayers;
  private final Captains captains;
  private final Teams teams;
  private final PickInventory pickInventory;
  private final LanguageManager languageManager;

  public AddCommand(
      AvailablePlayers availablePlayers,
      Captains captains,
      Teams teams,
      LanguageManager languageManager,
      PickInventory pickInventory) {
    this.availablePlayers = availablePlayers;
    this.captains = captains;
    this.teams = teams;
    this.pickInventory = pickInventory;
    this.languageManager = languageManager;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      SendMessage.sendToConsole(languageManager.getPluginMessage("errors.noPlayer"));
      return true;
    }
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer((Player) sender);
    if (!Draft.isDraftActive()) {
      matchPlayer.sendWarning(Component.text(languageManager.getPluginMessage("picks.noDraft")));
      return true;
    }

    if (args.length < 1) {
      matchPlayer.sendWarning(Component.text(languageManager.getPluginMessage("add.usage")));
      return true;
    }

    if (ConfigManager.getRankedTables().contains(ConfigManager.getTempTable())) {
      matchPlayer.sendWarning(
          Component.text(languageManager.getPluginMessage("ranked.notAllowed")));
      return true;
    }

    String playerName = args[0];

    if (isInvalidPlayer(playerName, sender)) {
      return true;
    }

    availablePlayers.addPlayer(playerName);
    pickInventory.updateAllInventories();
    SendMessage.broadcast(
        languageManager.getConfigurableMessage("picks.add").replace("{player}", playerName));
    PGM.get().getMatchManager().getMatch(sender).getMatch().playSound(Sounds.ALERT);
    return true;
  }

  private boolean isInvalidPlayer(String playerName, CommandSender sender) {
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer((Player) sender);
    if ((captains.getCaptain1Name() != null
            && captains.getCaptain1Name().equalsIgnoreCase(playerName))
        || (captains.getCaptain2Name() != null
            && captains.getCaptain2Name().equalsIgnoreCase(playerName))) {
      matchPlayer.sendWarning(Component.text(languageManager.getPluginMessage("add.captain")));
      return true;
    }

    if (teams.isPlayerInAnyTeam(playerName)) {
      matchPlayer.sendWarning(
          Component.text(languageManager.getPluginMessage("captains.alreadyInTeam")));
      return true;
    }

    if (availablePlayers.getAllAvailablePlayers().contains(playerName)) {
      matchPlayer.sendWarning(
          Component.text(languageManager.getPluginMessage("captains.alreadyInDraft")));
      return true;
    }
    return false;
  }
}
