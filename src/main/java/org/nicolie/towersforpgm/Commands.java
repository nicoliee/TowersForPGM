package org.nicolie.towersforpgm;

import org.nicolie.towersforpgm.commands.AddCommand;
import org.nicolie.towersforpgm.commands.BalanceCommand;
import org.nicolie.towersforpgm.commands.CancelMatchCommand;
import org.nicolie.towersforpgm.commands.CaptainsCommand;
import org.nicolie.towersforpgm.commands.PickCommand;
import org.nicolie.towersforpgm.commands.PreparationTimeCommand;
import org.nicolie.towersforpgm.commands.ReadyCommand;
import org.nicolie.towersforpgm.commands.RemoveCommand;
import org.nicolie.towersforpgm.commands.TowersCommand;
import org.nicolie.towersforpgm.commands.TowersForPGMCommand;
import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.Matchmaking;
import org.nicolie.towersforpgm.draft.Teams;
import org.nicolie.towersforpgm.gui.Picks;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import org.nicolie.towersforpgm.refill.RefillManager;

public class Commands {

  private final TowersForPGM plugin;

  public Commands(TowersForPGM plugin) {
    this.plugin = plugin;
  }

  public void registerCommands(
      AvailablePlayers availablePlayers,
      Captains captains,
      Draft draft,
      Matchmaking matchmaking,
      Picks pickInventory,
      RefillManager refillManager,
      Teams teams,
      PreparationListener preparationListener) {
    plugin
        .getCommand("add")
        .setExecutor(new AddCommand(availablePlayers, captains, teams, pickInventory));
    plugin.getCommand("balance").setExecutor(new BalanceCommand(matchmaking));
    plugin.getCommand("cancelMatch").setExecutor(new CancelMatchCommand());
    plugin.getCommand("captains").setExecutor(new CaptainsCommand(draft));
    ;
    plugin
        .getCommand("pick")
        .setExecutor(new PickCommand(draft, captains, availablePlayers, teams, pickInventory));
    plugin
        .getCommand("preparationTime")
        .setExecutor(new PreparationTimeCommand(preparationListener));
    plugin.getCommand("towers").setExecutor(new TowersCommand());
    plugin.getCommand("ready").setExecutor(new ReadyCommand(captains));
    plugin
        .getCommand("remove")
        .setExecutor(new RemoveCommand(draft, teams, captains, availablePlayers, pickInventory));
    plugin.getCommand("towersForPGM").setExecutor(new TowersForPGMCommand(plugin));
  }
}
