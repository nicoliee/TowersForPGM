package org.nicolie.towersforpgm;
import org.nicolie.towersforpgm.commands.AddCommand;
import org.nicolie.towersforpgm.commands.CaptainsCommand;
import org.nicolie.towersforpgm.commands.ConfigCommand;
import org.nicolie.towersforpgm.commands.PickCommand;
import org.nicolie.towersforpgm.commands.PreparationTimeCommand;
import org.nicolie.towersforpgm.commands.PrivateMatchCommand;
import org.nicolie.towersforpgm.commands.ReadyCommand;
import org.nicolie.towersforpgm.commands.RefillCommand;
import org.nicolie.towersforpgm.commands.RemoveCommand;
import org.nicolie.towersforpgm.commands.SetTableCommand;
import org.nicolie.towersforpgm.commands.StatsCommand;
import org.nicolie.towersforpgm.commands.TableCommand;
import org.nicolie.towersforpgm.commands.TopCommand;
import org.nicolie.towersforpgm.commands.TowersForPGMCommand;
import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.Teams;
import org.nicolie.towersforpgm.preparationTime.TorneoListener;
import org.nicolie.towersforpgm.refill.RefillManager;


public class Commands {
    
    private final TowersForPGM plugin;

    public Commands(TowersForPGM plugin) {
        this.plugin = plugin;
    }

    public void registerCommands(AvailablePlayers availablePlayers, Captains captains, Draft draft, MatchManager matchManager, RefillManager refillManager, Teams teams, TorneoListener torneoListener) {
        plugin.getCommand("add").setExecutor(new AddCommand(availablePlayers, captains, draft, teams, plugin));
        plugin.getCommand("captains").setExecutor(new CaptainsCommand(plugin, draft, matchManager));
        plugin.getCommand("pick").setExecutor(new PickCommand(draft, captains, availablePlayers, plugin));
        plugin.getCommand("preparationTime").setExecutor(new PreparationTimeCommand(plugin, torneoListener, matchManager));
        plugin.getCommand("privateMatch").setExecutor(new PrivateMatchCommand(plugin, matchManager));
        plugin.getCommand("config").setExecutor(new ConfigCommand(plugin, matchManager));
        plugin.getCommand("ready").setExecutor(new ReadyCommand(plugin, captains));
        plugin.getCommand("refill").setExecutor(new RefillCommand(refillManager, matchManager));
        plugin.getCommand("remove").setExecutor(new RemoveCommand(plugin, draft, teams, captains, availablePlayers));
        plugin.getCommand("setTable").setExecutor(new SetTableCommand(plugin, matchManager));
        plugin.getCommand("stat").setExecutor(new StatsCommand(plugin, matchManager));
        plugin.getCommand("table").setExecutor(new TableCommand(plugin));
        plugin.getCommand("top").setExecutor(new TopCommand(plugin, matchManager));
        plugin.getCommand("towersForPGM").setExecutor(new TowersForPGMCommand(plugin));
    }
}
