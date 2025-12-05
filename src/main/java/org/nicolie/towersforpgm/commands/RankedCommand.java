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
import org.nicolie.towersforpgm.configs.tables.TableType;
import org.nicolie.towersforpgm.draft.Utilities;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.rankeds.listeners.QueueJoinListener;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class RankedCommand implements CommandExecutor, TabCompleter {
  TowersForPGM plugin = TowersForPGM.getInstance();
  private final Utilities utilities;
  private final Queue queue;
  private static Boolean RANKED_AVAILABLE = TowersForPGM.getInstance().getIsDatabaseActivated()
      || !TowersForPGM.getInstance()
          .config()
          .databaseTables()
          .getTables(TableType.RANKED)
          .isEmpty();

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
      sender.sendMessage(LanguageManager.message("ranked.usage"));
      return true;
    }

    if (!RANKED_AVAILABLE) {
      PGM.get()
          .getMatchManager()
          .getPlayer((Player) sender)
          .sendWarning(Component.text(LanguageManager.message("ranked.unavailable")));
      return true;
    }

    String mainArg = args[0].toLowerCase();
    switch (mainArg) {
      case "join":
      case "leave":
        if (!(sender instanceof Player)) {
          sender.sendMessage(LanguageManager.message("errors.noPlayer"));
          return true;
        }
        if (MatchBotConfig.isVoiceChatEnabled()) return true;
        Match match = PGM.get().getMatchManager().getMatch(sender);
        MatchPlayer player = match.getPlayer((Player) sender);
        if (mainArg.equals("join")) {
          queue.addPlayer(player);
        } else {
          queue.removePlayer(player);
        }
        break;
      case "list":
        String prefix = LanguageManager.message("ranked.prefix");
        Match listMatch = sender instanceof Player
            ? PGM.get().getMatchManager().getMatch(sender)
            : PGM.get().getMatchManager().getMatches().next();
        int targetSize = queue.getTargetSize(listMatch);

        String header = prefix
            + LanguageManager.message("ranked.queueHeader")
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
          sender.sendMessage(LanguageManager.message("errors.noPermission"));
          return true;
        }

        // delegar la lógica de recolección desde voice al listener
        QueueJoinListener.reloadQueueFromVoice(queue);
        break;
      case "time":
        if (!sender.hasPermission("towers.admin")) {
          sender.sendMessage(LanguageManager.message("errors.noPermission"));
          return true;
        }

        if (args.length < 2) {
          sender.sendMessage(LanguageManager.message("ranked.time.usage"));
          return true;
        }

        Match timeMatch = sender instanceof Player
            ? PGM.get().getMatchManager().getMatch(sender)
            : PGM.get().getMatchManager().getMatches().next();

        String timeArg = args[1].toLowerCase();
        if (timeArg.equals("cancel")) {
          if (queue.cancelManualRanked(timeMatch)) {
            sender.sendMessage(LanguageManager.message("ranked.time.cancelled"));
          } else {
            sender.sendMessage(LanguageManager.message("ranked.time.noCountdown"));
          }
        } else {
          try {
            int seconds = Integer.parseInt(timeArg);
            if (seconds < 1 || seconds > 300) {
              sender.sendMessage(LanguageManager.message("ranked.time.invalidRange"));
              return true;
            }

            if (queue.startManualRanked(timeMatch, seconds)) {
              sender.sendMessage(LanguageManager.message("ranked.time.started")
                  .replace("{time}", String.valueOf(seconds)));
            } else {
              sender.sendMessage(LanguageManager.message("ranked.time.cannotStart"));
            }
          } catch (NumberFormatException e) {
            sender.sendMessage(LanguageManager.message("ranked.time.invalidNumber"));
          }
        }
        break;
      default:
        sender.sendMessage(LanguageManager.message("ranked.usage"));
        break;
    }
    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      List<String> options = Arrays.asList("join", "leave", "list", "reload", "time");
      return options.stream()
          .filter(option -> option.startsWith(args[0].toLowerCase()))
          .collect(Collectors.toList());
    } else if (args.length == 2 && args[0].equalsIgnoreCase("time")) {
      List<String> timeOptions = Arrays.asList("cancel", "30", "60", "90", "120");
      return timeOptions.stream()
          .filter(option -> option.startsWith(args[1].toLowerCase()))
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }
}
