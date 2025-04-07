package org.nicolie.towersforpgm.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.MatchManager;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.SendMessage;

public class TopCommand implements CommandExecutor, TabCompleter {
    private final TowersForPGM plugin;
    private final MatchManager matchManager;

    public TopCommand(TowersForPGM plugin, MatchManager matchManager) {
        this.plugin = plugin;
        this.matchManager = matchManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPluginMessage("errors.noPlayer"));
            return true;
        }
        Player player = (Player) sender;
        if (args.length < 1 || args.length > 3) {
            sender.sendMessage(plugin.getPluginMessage("top.usage"));
            return true;
        }

        String category = args[0].toLowerCase();
        if (!category.matches("kills|deaths|points|wins|games")) {
            SendMessage.sendToPlayer(player, plugin.getPluginMessage("top.invalidCategory"));
            return true;
        }

        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
                if (page < 1) {
                    SendMessage.sendToPlayer(player, plugin.getPluginMessage("top.invalidPage"));
                    return true;
                }
            } catch (NumberFormatException e) {
                SendMessage.sendToPlayer(player, plugin.getPluginMessage("top.invalidPage"));
                return true;
            }
        }
        String table = (args.length == 3) ? args[2] : ConfigManager.getTableForMap(matchManager.getMatch().getMap().getName());
        if (!ConfigManager.getTables().contains(table)) {
            SendMessage.sendToPlayer(player, plugin.getPluginMessage("stats.tableNotFound"));
            return true;
        }
        StatsManager.showTop(category, page, table, sender);
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterSuggestions(args[0], Arrays.asList("kills", "deaths", "assists", "points", "wins", "games"));
        }
        if (args.length == 2) {
            return Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"); // Sin filtro
        }
        if (args.length == 3) {
            return filterSuggestions(args[2], ConfigManager.getTables());
        }
        return Collections.emptyList();
    }

    private List<String> filterSuggestions(String input, List<String> options) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}