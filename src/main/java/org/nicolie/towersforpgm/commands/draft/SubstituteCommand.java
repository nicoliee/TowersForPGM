package org.nicolie.towersforpgm.commands.draft;

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
import org.nicolie.towersforpgm.draft.components.DraftPhase;
import org.nicolie.towersforpgm.draft.components.SubstituteResult;
import org.nicolie.towersforpgm.draft.core.Draft;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

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

    if (Draft.getPhase() == DraftPhase.IDLE) {
      matchPlayer.sendWarning(Component.text(LanguageManager.message("draft.picks.noDraft")));
      return true;
    }

    if (args.length < 2) {
      matchPlayer.sendWarning(Component.text(LanguageManager.message("draft.substitute.usage")));
      return true;
    }

    String teamName = args[0];
    String newCaptainName = args[1];

    // Determinar el número de equipo basado en el nombre del equipo
    int teamNumber = -1;
    if (teams.getTeamName(1).equalsIgnoreCase(teamName)) {
      teamNumber = 1;
    } else if (teams.getTeamName(2).equalsIgnoreCase(teamName)) {
      teamNumber = 2;
    }

    if (teamNumber == -1) {
      matchPlayer.sendWarning(Component.text(
          LanguageManager.message("draft.substitute.invalidTeam").replace("{team}", teamName)));
      return true;
    }

    Player newCaptain = Bukkit.getPlayerExact(newCaptainName);

    if (newCaptain == null) {
      matchPlayer.sendWarning(
          Component.text(LanguageManager.message("draft.substitute.notConnected")
              .replace("{player}", newCaptainName)));
      return true;
    }

    SubstituteResult result = draft.substituteCaptainByTeam(teamNumber, newCaptain.getUniqueId());

    switch (result) {
      case SUCCESS:
        String successMessage = LanguageManager.message("draft.substitute.success")
            .replace("{player}", newCaptain.getName())
            .replace("{team}", teams.getTeamName(teamNumber));
        match.sendMessage(Component.text(successMessage));
        match.playSound(Sounds.TIP);
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
    if (Draft.getPhase() == DraftPhase.IDLE) {
      return Collections.emptyList();
    }

    if (args.length == 1) {
      // Sugerir nombres de equipos para el primer argumento
      return getTeamNameSuggestions(args[0]);
    } else if (args.length == 2) {
      // Sugerir jugadores según el equipo seleccionado
      return getSubstituteSuggestions(args[0], args[1]);
    }
    return Collections.emptyList();
  }

  private List<String> getTeamNameSuggestions(String input) {
    return java.util.Arrays.asList(teams.getTeamName(1), teams.getTeamName(2)).stream()
        .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
        .collect(Collectors.toList());
  }

  private List<String> getSubstituteSuggestions(String teamName, String input) {
    // Determinar el número de equipo
    int teamNumber = -1;
    if (teams.getTeamName(1).equalsIgnoreCase(teamName)) {
      teamNumber = 1;
    } else if (teams.getTeamName(2).equalsIgnoreCase(teamName)) {
      teamNumber = 2;
    }

    if (teamNumber == -1) {
      return Collections.emptyList();
    }

    // Sugerir jugadores disponibles
    List<String> suggestions = draft.getAvailablePlayers().getAllAvailablePlayers().stream()
        .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
        .collect(Collectors.toList());

    // Sugerir jugadores del mismo equipo (excepto el capitán actual)
    UUID currentCaptainUUID = draft.getCaptainByTeam(teamNumber);
    String currentCaptainName =
        currentCaptainUUID != null ? Bukkit.getOfflinePlayer(currentCaptainUUID).getName() : null;

    draft.getTeams().getAllTeam(teamNumber).stream()
        .filter(name -> !name.equals(currentCaptainName))
        .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
        .forEach(suggestions::add);

    return suggestions;
  }
}
