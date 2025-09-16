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
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

public class TopCommand implements CommandExecutor, TabCompleter {
  private final LanguageManager languageManager;

  public TopCommand(LanguageManager languageManager) {
    this.languageManager = languageManager;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!TowersForPGM.getInstance().getIsDatabaseActivated()) {
      return true;
    }
    if (!(sender instanceof Player)) {
      SendMessage.sendToConsole(languageManager.getPluginMessage("errors.noPlayer"));
      return true;
    }
    if (!(sender instanceof Player)) {
      sender.sendMessage(languageManager.getPluginMessage("errors.noPlayer"));
      return true;
    }
    Player player = (Player) sender;
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    if (args.length < 1 || args.length > 3) {
      matchPlayer.sendWarning(Component.text(languageManager.getPluginMessage("top.usage")));
      return true;
    }

    String category = args[0].toLowerCase();
    if (!category.matches(
        "elo|maxelo|kills|deaths|assists|damagedone|damagetaken|points|wins|games")) {
      matchPlayer.sendWarning(
          Component.text(languageManager.getPluginMessage("top.invalidCategory")));
      return true;
    }

    int page = 1;
    if (args.length >= 2) {
      try {
        page = Integer.parseInt(args[1]);
        if (page < 1) {
          matchPlayer.sendWarning(
              Component.text(languageManager.getPluginMessage("top.invalidPage")));
          return true;
        }
      } catch (NumberFormatException e) {
        matchPlayer.sendWarning(
            Component.text(languageManager.getPluginMessage("top.invalidPage")));
        return true;
      }
    }
    String table = (args.length == 3)
        ? args[2]
        : ConfigManager.getTableForMap(
            PGM.get().getMatchManager().getMatch(sender).getMap().getName());
    if (!ConfigManager.getTables().contains(table)) {
      matchPlayer.sendWarning(
          Component.text(languageManager.getPluginMessage("stats.tableNotFound")));
      return true;
    }
    StatsManager.showTop(category, page, table, sender, languageManager);
    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      return filterSuggestions(
          args[0],
          Arrays.asList(
              "elo",
              "maxelo",
              "kills",
              "deaths",
              "assists",
              "damageDone",
              "damageTaken",
              "points",
              "wins",
              "games"));
    }
    if (args.length == 2) {
      return Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
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
