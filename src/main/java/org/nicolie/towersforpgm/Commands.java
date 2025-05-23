package org.nicolie.towersforpgm;
import org.nicolie.towersforpgm.commands.AddCommand;
import org.nicolie.towersforpgm.commands.CaptainsCommand;
import org.nicolie.towersforpgm.commands.PickCommand;
import org.nicolie.towersforpgm.commands.PreparationTimeCommand;
import org.nicolie.towersforpgm.commands.ReadyCommand;
import org.nicolie.towersforpgm.commands.RefillCommand;
import org.nicolie.towersforpgm.commands.RemoveCommand;
import org.nicolie.towersforpgm.commands.StatsCommand;
import org.nicolie.towersforpgm.commands.TopCommand;
import org.nicolie.towersforpgm.commands.TowersCommand;
import org.nicolie.towersforpgm.commands.TowersForPGMCommand;
import org.nicolie.towersforpgm.commands.TurnCommand;
import org.nicolie.towersforpgm.draft.AvailablePlayers;
import org.nicolie.towersforpgm.draft.Captains;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.PickInventory;
import org.nicolie.towersforpgm.draft.Teams;
import org.nicolie.towersforpgm.preparationTime.TorneoListener;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.utils.LanguageManager;


public class Commands {
    
    private final TowersForPGM plugin;

    public Commands(TowersForPGM plugin) {
        this.plugin = plugin;
    }

    public void registerCommands(AvailablePlayers availablePlayers, Captains captains, Draft draft, LanguageManager languageManager, PickInventory pickInventory, RefillManager refillManager, Teams teams, TorneoListener torneoListener) {
        plugin.getCommand("add").setExecutor(new AddCommand(availablePlayers, captains, teams, languageManager, pickInventory));
        plugin.getCommand("captains").setExecutor(new CaptainsCommand(draft, languageManager, pickInventory));;
        plugin.getCommand("pick").setExecutor(new PickCommand(draft, captains, availablePlayers, teams, languageManager, pickInventory));
        plugin.getCommand("preparationTime").setExecutor(new PreparationTimeCommand(languageManager, torneoListener));
        plugin.getCommand("towers").setExecutor(new TowersCommand(languageManager));
        plugin.getCommand("ready").setExecutor(new ReadyCommand(captains, languageManager));
        plugin.getCommand("refill").setExecutor(new RefillCommand(languageManager, refillManager));
        plugin.getCommand("remove").setExecutor(new RemoveCommand(draft, teams, captains, availablePlayers, languageManager, pickInventory));
        plugin.getCommand("stat").setExecutor(new StatsCommand(languageManager));
        plugin.getCommand("top").setExecutor(new TopCommand(languageManager));
        plugin.getCommand("towersForPGM").setExecutor(new TowersForPGMCommand(plugin, languageManager));
        plugin.getCommand("turn").setExecutor(new TurnCommand(draft, captains, languageManager));
    }
}