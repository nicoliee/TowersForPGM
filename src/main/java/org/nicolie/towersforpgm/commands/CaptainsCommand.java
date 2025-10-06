package org.nicolie.towersforpgm.commands;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

// Comando para iniciar un draft de capitanes, actualmente solo soporta dos equipos "red" y "blue"
// PGM actualmente solo soporta una partida a la vez, por lo que no se pueden realizar múltiples
// drafts simultáneamente
public class CaptainsCommand implements CommandExecutor, TabCompleter {
  private final Draft draft;

  public CaptainsCommand(Draft draft) {
    this.draft = draft;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(LanguageManager.langMessage("errors.noPlayer"));
      return true;
    }
    Match match = PGM.get().getMatchManager().getMatch(sender);
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer((Player) sender);
    // Verificar si la partida está en curso
    if (match.isRunning() || match.isFinished()) {
      matchPlayer.sendWarning(
          Component.text(LanguageManager.langMessage("draft.captains.matchStarted")));
      return true;
    }

    // Verificar si los argumentos son suficientes
    if (args.length < 2) {
      matchPlayer.sendWarning(Component.text(LanguageManager.langMessage("draft.captains.usage")));
      return true;
    }

    // Verificar si hay suficientes jugadores en línea
    if (Bukkit.getOnlinePlayers().size() < 2) {
      matchPlayer.sendWarning(
          Component.text(LanguageManager.langMessage("draft.captains.notEnoughPlayers")));
      return true;
    }

    // Evitar que los argumentos sean iguales (duplicados)
    if (args[0].equalsIgnoreCase(args[1])) {
      matchPlayer.sendWarning(Component.text(LanguageManager.langMessage("draft.captains.usage")));
      return true;
    }

    // Verificar si los capitanes están en línea
    if (Bukkit.getPlayer(args[0]) == null || Bukkit.getPlayer(args[1]) == null) {
      matchPlayer.sendWarning(
          Component.text(LanguageManager.langMessage("draft.captains.offline")));
      return true;
    }

    // Verificar que no sea una ranked
    if (Queue.isRanked()) {
      matchPlayer.sendWarning(Component.text(LanguageManager.langMessage("ranked.notAllowed")));
      return true;
    }

    UUID captain1 = Bukkit.getPlayer(args[0]).getUniqueId();
    UUID captain2 = Bukkit.getPlayer(args[1]).getUniqueId();

    boolean randomizeOrder = true;
    if (args.length >= 3 && args[2].equalsIgnoreCase("force")) {
      randomizeOrder = false;
    } else {
      randomizeOrder = true;
    }

    // Crear una lista de jugadores en línea excluyendo a los capitanes
    List<MatchPlayer> onlinePlayersExcludingCaptains = match.getPlayers().stream()
        .filter(player -> !player.getId().equals(captain1) && !player.getId().equals(captain2))
        .collect(Collectors.toList());

    // Iniciar el draft con los capitanes y los jugadores restantes
    draft.setCustomOrderPattern(ConfigManager.getDraftOrder(), ConfigManager.getMinDraftOrder());
    draft.startDraft(captain1, captain2, onlinePlayersExcludingCaptains, match, randomizeOrder);
    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    if (!(sender instanceof Player)) return Collections.emptyList();

    if (args.length == 1) {
      return Bukkit.getOnlinePlayers().stream()
          .map(Player::getName)
          .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
          .collect(Collectors.toList());
    }

    if (args.length == 2) {
      return Bukkit.getOnlinePlayers().stream()
          .map(Player::getName)
          .filter(name -> !name.equalsIgnoreCase(args[0])) // Evitar duplicar capitán 1
          .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
          .collect(Collectors.toList());
    }

    // Tercer argumento: solo "force"
    if (args.length == 3) {
      if ("force".startsWith(args[2].toLowerCase())) {
        return Collections.singletonList("force");
      }
      return Collections.emptyList();
    }

    return Collections.emptyList();
  }
}
