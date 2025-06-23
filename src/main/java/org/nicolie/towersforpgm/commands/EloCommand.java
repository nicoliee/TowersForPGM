package org.nicolie.towersforpgm.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.rankeds.Rank;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

public class EloCommand implements CommandExecutor{
    private final LanguageManager languageManager;

    public EloCommand(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!TowersForPGM.getInstance().getIsDatabaseActivated()) { return true;}
        if (!(sender instanceof Player)) {
            SendMessage.sendToConsole(languageManager.getPluginMessage("errors.noPlayer"));
            return true;
        }
        String table = ConfigManager.getRankedDefaultTable();
        if (args.length > 0 && args[0].equalsIgnoreCase("lb")) {
            int page = 1;
            if (args.length > 1) {
                try {
                    page = Integer.parseInt(args[1]);
                    if (page < 1) page = 1;
                } catch (NumberFormatException e) {
                    page = 1;
                }
            }
            StatsManager.showTop("elo", page, table, sender, languageManager);
            return true;
        }
        String player;
        if (args.length > 0) {
            player = args[0];
        } else if (sender.getName() != null && sender instanceof org.bukkit.entity.Player) {
            player = sender.getName();
        } else {
            sender.sendMessage("§c/elo <nick>");
            return true;
        }
        StatsManager.getEloForUsername(table, player, eloChange -> {
            if (eloChange == null) {
                sender.sendMessage(languageManager.getPluginMessage("stats.noStats"));
            } else {
                int elo = eloChange.getCurrentElo();
                int maxElo = eloChange.getMaxElo();
                sender.sendMessage(Queue.RANKED_PREFIX + Rank.getRankByElo(elo).getPrefixedRank(true)
                        + Rank.getRankByElo(elo).getColor() + " " + player + " §r- Elo: " + Rank.getRankByElo(elo).getColor() + elo +" §f| Max Elo: " + Rank.getRankByElo(maxElo).getPrefixedRank(true) + " " + Rank.getRankByElo(maxElo).getColor() + maxElo);
            }
        });
        return true;
    }
}
