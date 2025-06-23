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
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.Utilities;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;

import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class RankedCommand implements CommandExecutor, TabCompleter{
    private final Utilities utilities;
    private final Queue queue;
    private final LanguageManager languageManager;
    public RankedCommand(LanguageManager languageManager, Queue queue, Utilities utilities) {
        this.languageManager = languageManager;
        this.utilities = utilities;
        this.queue = queue;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!TowersForPGM.getInstance().getIsDatabaseActivated()){return true;}
        if (args.length == 0) {
            sender.sendMessage("§c/ranked <join|leave|list>");
            return true;
        }
        
        String mainArg = args[0].toLowerCase();
        switch (mainArg) {
            case "join":
            case "leave":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(languageManager.getPluginMessage("errors.noPlayer"));
                    return true;
                }
                Match match = PGM.get().getMatchManager().getMatch(sender);
                MatchPlayer player = match.getPlayer((Player) sender);
                if (mainArg.equals("join")) {
                    queue.addPlayer(player);
                } else {
                    queue.removePlayer(player);
                }
                break;
            case "list":
                if (sender instanceof Player) {
                    Match matchList = PGM.get().getMatchManager().getMatch(sender);
                    MatchPlayer playerList = matchList.getPlayer((Player) sender);
                    List<String> queuePlayers = queue.getQueueList();
                    playerList.sendMessage(Component.text("§8[§6Ranked§8] §a" + queue.getQueueSize() + "/" + ConfigManager.getRankedSize()));
                    if (!queuePlayers.isEmpty()) {
                        StringBuilder playersList = utilities.buildLists(queuePlayers, "", false);
                        playerList.sendMessage(Component.text(playersList.toString()));
                    }
                } else {
                    List<String> queuePlayers = queue.getQueueList();
                    sender.sendMessage("§8[§6Ranked§8] §a" + queue.getQueueSize() + "/" + ConfigManager.getRankedSize());
                    if (!queuePlayers.isEmpty()) {
                        StringBuilder playersList = utilities.buildLists(queuePlayers, "", false);
                        sender.sendMessage(playersList.toString());
                    }
                }
                break;
            case "size":
                if (!sender.hasPermission("ranked.size")) {
                    sender.sendMessage(languageManager.getPluginMessage("errors.noPermission"));
                    return true;
                }
                if (args.length == 1) {
                    sender.sendMessage("§c/ranked size <size>");
                    return true;
                }
                int size;
                try {
                    size = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(languageManager.getPluginMessage("ranked.sizeInvalid"));
                    return true;
                }
                queue.setSize(sender, size);
                break;
            default:
                sender.sendMessage("§c/ranked <join|leave|list>");
                break;
        }
        return true;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("join", "leave", "list", "size");
            return options.stream()
                          .filter(option -> option.startsWith(args[0].toLowerCase()))
                          .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
