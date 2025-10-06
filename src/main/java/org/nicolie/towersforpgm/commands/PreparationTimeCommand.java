package org.nicolie.towersforpgm.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.preparationTime.PreparationListener;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class PreparationTimeCommand implements CommandExecutor, TabCompleter {
  private final PreparationListener preparationListener;

  public PreparationTimeCommand(PreparationListener preparationListener) {
    this.preparationListener = preparationListener;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Player player = (Player) sender;
    // Verificar que el comando lo ejecute un jugador
    if (!(sender instanceof Player)) {
      SendMessage.sendToPlayer(player, LanguageManager.langMessage("errors.noPlayer"));
      return true;
    }

    // Verificar si no hay argumentos
    if (args.length == 0) {
      return true;
    }

    Match match = PGM.get().getMatchManager().getMatch(player);
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    // Procesar el comando dependiendo del primer argumento
    String action = args[0].toLowerCase();

    switch (action) {
      case "on":
        preparationListener.startProtection(player, match);
        break;

      case "off":
        preparationListener.stopProtection(player, match);
        break;

      default:
        matchPlayer.sendWarning(Component.text(LanguageManager.langMessage("preparation.usage")));
        return true;
    }
    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      List<String> options = Arrays.asList("on", "off");
      String input = args[0].toLowerCase();
      List<String> filteredOptions = new ArrayList<>();
      for (String option : options) {
        if (option.toLowerCase().startsWith(input)) {
          filteredOptions.add(option);
        }
      }
      return filteredOptions;
    }
    return null;
  }
}
