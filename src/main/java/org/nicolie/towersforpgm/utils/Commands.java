package org.nicolie.towersforpgm.utils;

import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.commands.CancelMatchCommand;
import org.nicolie.towersforpgm.commands.SudoCommand;
import org.nicolie.towersforpgm.commands.TowersCommand;
import org.nicolie.towersforpgm.commands.TowersForPGMCommand;
import org.nicolie.towersforpgm.commands.draft.AddCommand;
import org.nicolie.towersforpgm.commands.draft.BalanceCommand;
import org.nicolie.towersforpgm.commands.draft.CaptainsCommand;
import org.nicolie.towersforpgm.commands.draft.PickCommand;
import org.nicolie.towersforpgm.commands.draft.ReadyCommand;
import org.nicolie.towersforpgm.commands.draft.RemoveCommand;
import org.nicolie.towersforpgm.commands.draft.SubstituteCommand;
import org.nicolie.towersforpgm.commands.ranked.EloCommand;
import org.nicolie.towersforpgm.commands.ranked.LinkCommand;
import org.nicolie.towersforpgm.draft.core.AvailablePlayers;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Draft;
import org.nicolie.towersforpgm.draft.core.Matchmaking;
import org.nicolie.towersforpgm.draft.core.Teams;
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
      RefillManager refillManager,
      Teams teams,
      PreparationListener preparationListener) {
    plugin.getCommand("add").setExecutor(new AddCommand(availablePlayers, captains, teams));
    plugin.getCommand("balance").setExecutor(new BalanceCommand(matchmaking));
    plugin.getCommand("cancelMatch").setExecutor(new CancelMatchCommand());
    plugin.getCommand("captains").setExecutor(new CaptainsCommand(draft));
    plugin.getCommand("elo").setExecutor(new EloCommand());
    plugin.getCommand("link").setExecutor(new LinkCommand());
    plugin
        .getCommand("pick")
        .setExecutor(new PickCommand(draft, captains, availablePlayers, teams));
    plugin.getCommand("towers").setExecutor(new TowersCommand());
    plugin.getCommand("ready").setExecutor(new ReadyCommand(captains, teams));
    plugin
        .getCommand("remove")
        .setExecutor(new RemoveCommand(draft, teams, captains, availablePlayers));
    plugin.getCommand("sub").setExecutor(new SubstituteCommand(draft, teams));
    plugin.getCommand("sudo").setExecutor(new SudoCommand());
    plugin.getCommand("towersForPGM").setExecutor(new TowersForPGMCommand(plugin));
  }
}
