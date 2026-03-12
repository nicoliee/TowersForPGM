package org.nicolie.towersforpgm.commands.ranked;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.core.Utilities;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.rankeds.listeners.QueueJoinListener;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.rankeds.RankedProfile;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.Audience;

public class RankedCommand implements CommandExecutor, TabCompleter {
  TowersForPGM plugin = TowersForPGM.getInstance();
  private final Utilities utilities;
  private final Queue queue;

  public RankedCommand(Queue queue, Utilities utilities) {
    this.utilities = utilities;
    this.queue = queue;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Audience audience = Audience.get(sender);
    if (!TowersForPGM.getInstance().getIsDatabaseActivated()) {
      return true;
    }
    if (args.length == 0) {
      audience.sendWarning(Component.translatable(
          "command.incorrectUsage", Component.text("/ranked <join|leave|list|profile>")));
      return true;
    }

    String mainArg = args[0].toLowerCase();
    switch (mainArg) {
      case "join":
      case "leave":
        if (!(sender instanceof Player)) {
          audience.sendWarning(Component.translatable("command.onlyPlayers"));
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
        Component prefix = Queue.RANKED_PREFIX;
        Match listMatch = sender instanceof Player
            ? PGM.get().getMatchManager().getMatch(sender)
            : PGM.get().getMatchManager().getMatches().next();
        int targetSize = queue.getTargetSize(listMatch);
        Component header = prefix
            .append(Component.space())
            .append(Component.text(Queue.getQueueSize() + "/" + targetSize));
        List<String> queuePlayers = queue.getQueueList();

        if (sender instanceof Player) {
          Match matchList = PGM.get().getMatchManager().getMatch(sender);
          MatchPlayer playerList = matchList.getPlayer((Player) sender);
          playerList.sendMessage(header);
          if (!queuePlayers.isEmpty()) {
            Component playersList = utilities.buildLists(queuePlayers, NamedTextColor.WHITE, false);
            playerList.sendMessage(playersList);
          }
        } else {
          audience.sendMessage(header);
          if (!queuePlayers.isEmpty()) {
            Component playersList = utilities.buildLists(queuePlayers, NamedTextColor.WHITE, false);
            audience.sendMessage(playersList);
          }
        }
        break;
      case "reload":
        if (!sender.hasPermission("towers.admin")) {
          audience.sendMessage(Component.translatable("misc.noPermission"));
          return true;
        }
        QueueJoinListener.reloadQueueFromVoice();
        break;
      case "start":
        if (!sender.hasPermission("towers.admin")) {
          audience.sendMessage(Component.translatable("misc.noPermission"));
          return true;
        }
        queue.startCountdown(PGM.get().getMatchManager().getMatch(sender));
        break;
      case "stop":
        if (!sender.hasPermission("towers.admin")) {
          audience.sendMessage(Component.translatable("misc.noPermission"));
          return true;
        }
        queue.stopCountdown(PGM.get().getMatchManager().getMatch(sender));
        break;
      case "profile":
        if (args.length < 2) {
          // Show current active profile
          RankedProfile profile = plugin.config().ranked().getActiveProfile();
          if (profile != null) {
            audience.sendMessage(Component.text(profile.getFormattedInfo()));
          } else {
            audience.sendMessage(Component.translatable("ranked.noProfile"));
          }
        } else {
          // Show or set specific profile
          String profileName = args[1];
          if (!plugin.config().ranked().profileExists(profileName)) {
            audience.sendWarning(Component.translatable(
                "ranked.profileNotFound",
                Component.text("[" + profileName + "]").color(NamedTextColor.GRAY)));
            return true;
          }

          if (args.length >= 3 && args[2].equalsIgnoreCase("set")) {
            // Set active profile
            if (!sender.hasPermission("towers.admin")) {
              audience.sendMessage(Component.translatable("misc.noPermission"));
              return true;
            }
            plugin.config().ranked().setActiveProfile(profileName);
            audience.sendMessage(Component.translatable(
                "ranked.profileSet",
                Component.text("[" + profileName + "]").color(NamedTextColor.GRAY)));
          } else {
            // Show profile info
            RankedProfile profile = plugin.config().ranked().getProfile(profileName);
            if (profile != null) {
              audience.sendMessage(Component.text(profile.getFormattedInfo()));
            }
          }
        }
        break;
      default:
        audience.sendWarning(Component.translatable(
            "command.incorrectUsage", Component.text("/ranked <join|leave|list|profile>")));
        break;
    }
    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      List<String> options =
          Arrays.asList("join", "leave", "list", "reload", "start", "stop", "profile");
      return options.stream()
          .filter(option -> option.startsWith(args[0].toLowerCase()))
          .collect(Collectors.toList());
    } else if (args.length == 2 && args[0].equalsIgnoreCase("profile")) {
      // Return available profile names
      return plugin.config().ranked().getProfileNames().stream()
          .filter(profile -> profile.startsWith(args[1].toLowerCase()))
          .collect(Collectors.toList());
    } else if (args.length == 3 && args[0].equalsIgnoreCase("profile")) {
      return Collections.singletonList("set");
    }
    return Collections.emptyList();
  }
}
