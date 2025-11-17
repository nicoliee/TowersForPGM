package org.nicolie.towersforpgm.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.Utilities;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.rankeds.listeners.QueueJoinListener;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class RankedCommand implements CommandExecutor, TabCompleter {
  private final Utilities utilities;
  private final Queue queue;
  private static Boolean RANKED_AVAILABLE = TowersForPGM.getInstance().getIsDatabaseActivated()
      || !ConfigManager.getRankedTables().isEmpty();

  public RankedCommand(Queue queue, Utilities utilities) {
    this.utilities = utilities;
    this.queue = queue;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!TowersForPGM.getInstance().getIsDatabaseActivated()) {
      return true;
    }
    if (args.length == 0) {
      sender.sendMessage(LanguageManager.langMessage("ranked.usage"));
      return true;
    }

    if (!RANKED_AVAILABLE) {
      PGM.get()
          .getMatchManager()
          .getPlayer((Player) sender)
          .sendWarning(Component.text(LanguageManager.langMessage("ranked.unavailable")));
      return true;
    }

    String mainArg = args[0].toLowerCase();
    switch (mainArg) {
      case "join":
      case "leave":
        if (!(sender instanceof Player)) {
          sender.sendMessage(LanguageManager.langMessage("errors.noPlayer"));
          return true;
        }
        if (MatchBotConfig.isRankedEnabled()) return true;
        Match match = PGM.get().getMatchManager().getMatch(sender);
        MatchPlayer player = match.getPlayer((Player) sender);
        if (mainArg.equals("join")) {
          queue.addPlayer(player);
        } else {
          queue.removePlayer(player);
        }
        break;
      case "list":
        String prefix = LanguageManager.langMessage("ranked.prefix");
        Match listMatch = sender instanceof Player
            ? PGM.get().getMatchManager().getMatch(sender)
            : PGM.get().getMatchManager().getMatches().next();
        int targetSize = queue.getTargetSize(listMatch);

        String header = prefix
            + LanguageManager.langMessage("ranked.queueHeader")
                .replace("{current}", String.valueOf(Queue.getQueueSize()))
                .replace("{target}", String.valueOf(targetSize));
        List<String> queuePlayers = queue.getQueueList();

        if (sender instanceof Player) {
          Match matchList = PGM.get().getMatchManager().getMatch(sender);
          MatchPlayer playerList = matchList.getPlayer((Player) sender);
          playerList.sendMessage(Component.text(header));
          if (!queuePlayers.isEmpty()) {
            StringBuilder playersList = utilities.buildLists(queuePlayers, "", false);
            playerList.sendMessage(Component.text(playersList.toString()));
          }
        } else {
          sender.sendMessage(header);
          if (!queuePlayers.isEmpty()) {
            StringBuilder playersList = utilities.buildLists(queuePlayers, "", false);
            sender.sendMessage(playersList.toString());
          }
        }
        break;
      case "reload":
        // permiso exclusivo
        if (!sender.hasPermission("towers.admin")) {
          sender.sendMessage(LanguageManager.langMessage("errors.noPermission"));
          return true;
        }

        // delegar la lógica de recolección desde voice al listener
        QueueJoinListener.reloadQueueFromVoice(queue);
        break;
      default:
        sender.sendMessage(LanguageManager.langMessage("ranked.usage"));
        break;
    }
    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      List<String> options = Arrays.asList("join", "leave", "list", "reload");
      return options.stream()
          .filter(option -> option.startsWith(args[0].toLowerCase()))
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }
}
