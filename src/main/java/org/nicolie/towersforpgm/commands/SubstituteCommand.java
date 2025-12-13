package org.nicolie.towersforpgm.commands;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.draft.components.SubstituteResult;
import org.nicolie.towersforpgm.draft.core.Draft;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class SubstituteCommand implements CommandExecutor, TabCompleter {
  private final Draft draft;
  private final Teams teams;

  public SubstituteCommand(Draft draft, Teams teams) {
    this.draft = draft;
    this.teams = teams;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(LanguageManager.message("errors.noPlayer"));
      return true;
    }

    Match match = PGM.get().getMatchManager().getMatch(sender);
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer((Player) sender);

    if (!Draft.isDraftActive()) {
      matchPlayer.sendWarning(Component.text(LanguageManager.message("draft.picks.noDraft")));
      return true;
    }

    if (args.length < 2) {
      matchPlayer.sendWarning(Component.text(LanguageManager.message("draft.substitute.usage")));
      return true;
    }

    String currentCaptainName = args[0];
    String newCaptainName = args[1];

    Player currentCaptain = Bukkit.getPlayerExact(currentCaptainName);
    Player newCaptain = Bukkit.getPlayerExact(newCaptainName);

    if (currentCaptain == null) {
      matchPlayer.sendWarning(
          Component.text(LanguageManager.message("draft.substitute.notConnected")
              .replace("{player}", currentCaptainName)));
      return true;
    }

    if (newCaptain == null) {
      matchPlayer.sendWarning(
          Component.text(LanguageManager.message("draft.substitute.notConnected")
              .replace("{player}", newCaptainName)));
      return true;
    }

    SubstituteResult result =
        draft.substituteCaptain(currentCaptain.getUniqueId(), newCaptain.getUniqueId());

    switch (result) {
      case SUCCESS:
        int teamNumber = draft.getCaptainTeamNumber(newCaptain.getUniqueId());
        String teamName = teams.getTeamName(teamNumber);
        String successMessage = LanguageManager.message("draft.substitute.success")
            .replace("{player}", newCaptain.getName())
            .replace("{team}", teamName);
        matchPlayer.sendMessage(Component.text(successMessage));
        match.sendMessage(Component.text(successMessage));
        break;
      case NOT_CAPTAIN:
        matchPlayer.sendWarning(
            Component.text(LanguageManager.message("draft.substitute.notCaptain")));
        break;
      case ENEMY_TEAM:
        matchPlayer.sendWarning(
            Component.text(LanguageManager.message("draft.substitute.enemyTeam")));
        break;
      case NOT_AVAILABLE:
        matchPlayer.sendWarning(
            Component.text(LanguageManager.message("draft.substitute.notAvailable")));
        break;
    }

    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    if (!Draft.isDraftActive()) {
      return Collections.emptyList();
    }

    if (args.length == 1) {
      // Sugerir capitanes actuales para el primer argumento
      return getCaptainSuggestions(args[0]);
    } else if (args.length == 2) {
      // Sugerir jugadores según el capitán seleccionado
      return getSubstituteSuggestions(args[0], args[1]);
    }
    return Collections.emptyList();
  }

  private List<String> getCaptainSuggestions(String input) {
    return Bukkit.getOnlinePlayers().stream()
        .filter(player -> isCaptain(player))
        .map(Player::getName)
        .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
        .collect(Collectors.toList());
  }

  private List<String> getSubstituteSuggestions(String captainName, String input) {
    Player captain = Bukkit.getPlayerExact(captainName);
    if (captain == null || !isCaptain(captain)) {
      return Collections.emptyList();
    }

    List<String> suggestions = draft.getAvailablePlayers().getAllAvailablePlayers().stream()
        .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
        .collect(Collectors.toList());

    int teamNumber = draft.getCaptainTeamNumber(captain.getUniqueId());
    if (teamNumber != -1) {
      draft.getTeams().getAllTeam(teamNumber).stream()
          .filter(name -> !name.equals(captainName))
          .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
          .forEach(suggestions::add);
    }

    return suggestions;
  }

  private boolean isCaptain(Player player) {
    return draft.getCaptains().isCaptain(player.getUniqueId());
  }
}
