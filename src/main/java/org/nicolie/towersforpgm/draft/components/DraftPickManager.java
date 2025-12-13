package org.nicolie.towersforpgm.draft.components;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.ConfigManager;
import org.nicolie.towersforpgm.draft.core.AvailablePlayers;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.draft.core.Utilities;
import org.nicolie.towersforpgm.draft.events.DraftEndEvent;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.MatchManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.util.bukkit.Sounds;

/** Maneja picks, sustituciones y cierre de draft. */
public class DraftPickManager {
  private final TowersForPGM plugin;
  private final ConfigManager configManager;
  private final DraftState state;
  private final Captains captains;
  private final AvailablePlayers availablePlayers;
  private final Teams teams;
  private final Utilities utilities;
  private final DraftTurnManager turnManager;
  private DraftDisplayManager displayManager;

  public DraftPickManager(
      TowersForPGM plugin,
      ConfigManager configManager,
      DraftState state,
      Captains captains,
      AvailablePlayers availablePlayers,
      Teams teams,
      Utilities utilities,
      DraftTurnManager turnManager) {
    this.plugin = plugin;
    this.configManager = configManager;
    this.state = state;
    this.captains = captains;
    this.availablePlayers = availablePlayers;
    this.teams = teams;
    this.utilities = utilities;
    this.turnManager = turnManager;
  }

  public void setDisplayManager(DraftDisplayManager displayManager) {
    this.displayManager = displayManager;
  }

  public void pickPlayer(String username) {
    Player player = Bukkit.getPlayerExact(username);
    String exactUsername = availablePlayers.getExactUser(username);
    int teamNumber = captains.isCaptain1Turn() ? 1 : 2;
    String teamColor = teams.getTeamColor(teamNumber);
    String captainName =
        captains.isCaptain1Turn() ? captains.getCaptain1Name() : captains.getCaptain2Name();
    if (captainName == null) {
      captainName = teams.getTeamName(teamNumber);
    }
    Sound sound = captains.isCaptain1Turn() ? Sounds.MATCH_COUNTDOWN : Sounds.MATCH_START;

    teams.addPlayerToTeam(exactUsername, teamNumber);
    availablePlayers.removePlayer(exactUsername);
    teams.assignTeam(player, teamNumber);
    plugin.giveItem(player);

    SendMessage.broadcast(LanguageManager.message("draft.captains.choose")
        .replace("{teamcolor}", teamColor)
        .replace("{captain}", captainName)
        .replace("{player}", exactUsername));
    MatchManager.getMatch().playSound(sound);

    turnManager.updateTurnOrder();

    if (configManager.draft().isSecondPickBalance()) {
      if (availablePlayers.getAllAvailablePlayers().size() == 2) {
        Set<String> team1List = teams.getAllTeam(1);
        Set<String> team2List = teams.getAllTeam(2);
        int totalPlayers = team1List.size()
            + team2List.size()
            + availablePlayers.getAllAvailablePlayers().size();

        if (totalPlayers % 2 != 0) {
          List<String> lastTwoPlayers = new ArrayList<>(availablePlayers.getAllAvailablePlayers());

          int team1Size = team1List.size();
          int team2Size = team2List.size();
          int teamToAssign = team1Size < team2Size ? 1 : 2;

          for (String p : lastTwoPlayers) {
            Player pl = Bukkit.getPlayerExact(p);
            String exactName = availablePlayers.getExactUser(p);
            teams.addPlayerToTeam(exactName, teamToAssign);
            availablePlayers.removePlayer(exactName);
            teams.assignTeam(pl, teamToAssign);
            plugin.giveItem(pl);

            SendMessage.broadcast(LanguageManager.message("draft.captains.choose")
                .replace("{teamcolor}", teams.getTeamColor(teamToAssign))
                .replace(
                    "{captain}",
                    teamToAssign == 1 ? captains.getCaptain1Name() : captains.getCaptain2Name())
                .replace("{player}", exactName));
          }
          plugin.updateInventories();
          MatchManager.getMatch().playSound(Sounds.MATCH_START);
          endDraft();
          return;
        }
      }
    }

    if (availablePlayers.isEmpty()) {
      endDraft();
      return;
    }
    plugin.updateInventories();
    if (displayManager != null) {
      displayManager.startDraftTimer();
    }
  }

  public void endDraft() {
    if (state.getDraftTimer() != null) {
      state.getDraftTimer().cancel();
    }

    if (!state.isDraftActive()) {
      return;
    }

    state.setDraftActive(false);

    if (state.getDraftTimer() != null) {
      state.getDraftTimer().cancel();
    }

    List<String> team1Names = new ArrayList<>(teams.getAllTeam(1));
    List<String> team2Names = new ArrayList<>(teams.getAllTeam(2));

    StringBuilder team1 = utilities.buildLists(team1Names, teams.getTeamColor(1), false);
    StringBuilder team2 = utilities.buildLists(team2Names, teams.getTeamColor(2), false);
    int team1Size = team1Names.size();
    int team2Size = team2Names.size();
    int teamsize = Math.max(team1Size, team2Size);

    SendMessage.broadcast(LanguageManager.message("draft.captains.teamsHeader"));
    SendMessage.broadcast(team1.toString());
    SendMessage.broadcast("&8[" + teams.getTeamColor(1).replace("§", "&") + team1Size
        + "&8] &l&bvs. " + "&8[" + teams.getTeamColor(2).replace("§", "&") + team2Size + "&8]");
    SendMessage.broadcast(team2.toString());
    SendMessage.broadcast("§m------------------------------");

    teams.setTeamsSize(teamsize);

    captains.setReadyActive(true);
    captains.setMatchWithCaptains(true);

    plugin.removeItem(MatchManager.getMatch().getWorld());
    utilities.readyReminder(5, 20);

    if (displayManager != null) {
      displayManager.removeBossbar();
    }

    Set<String> team1Set = teams.getAllTeam(1);
    Set<String> team2Set = teams.getAllTeam(2);
    DraftEndEvent draftEndEvent = new DraftEndEvent(team1Set, team2Set, MatchManager.getMatch());
    Bukkit.getPluginManager().callEvent(draftEndEvent);

    MatchManager.getMatch()
        .needModule(StartMatchModule.class)
        .forceStartCountdown(Duration.ofSeconds(90), Duration.ZERO);
  }

  public void cleanLists() {
    captains.clear();
    availablePlayers.clear();
    teams.clear();
    state.setDraftActive(false);
    if (state.getDraftTimer() != null) {
      state.getDraftTimer().cancel();
      state.setDraftTimer(null);
    }
  }

  public SubstituteResult substituteCaptain(UUID currentCaptainUUID, UUID newCaptainUUID) {
    if (!state.isDraftActive() || !captains.isCaptain(currentCaptainUUID)) {
      return SubstituteResult.NOT_CAPTAIN;
    }

    Player currentCaptain = Bukkit.getPlayer(currentCaptainUUID);
    Player newCaptain = Bukkit.getPlayer(newCaptainUUID);
    if (currentCaptain == null || newCaptain == null) {
      return SubstituteResult.NOT_AVAILABLE;
    }

    int teamNumber = captains.getCaptainTeam(currentCaptainUUID);
    String newCaptainName = newCaptain.getName();
    String currentCaptainName = currentCaptain.getName();

    if (teams.isPlayerInTeam(newCaptainName, teamNumber == 1 ? 2 : 1)) {
      return SubstituteResult.ENEMY_TEAM;
    }

    if (!availablePlayers.isPlayerAvailable(newCaptainName)
        && !teams.isPlayerInTeam(newCaptainName, teamNumber)) {
      return SubstituteResult.NOT_AVAILABLE;
    }

    if (availablePlayers.isPlayerAvailable(newCaptainName)) {
      swapCaptainWithAvailablePlayer(
          currentCaptain, newCaptain, currentCaptainName, newCaptainName, teamNumber);
    }

    captains.substituteCaptain(currentCaptainUUID, newCaptainUUID);

    if ((teamNumber == 1 && captains.isCaptain1Turn())
        || (teamNumber == 2 && !captains.isCaptain1Turn())) {
      plugin.giveitem(MatchManager.getMatch().getWorld());
    }

    return SubstituteResult.SUCCESS;
  }

  public int getCaptainTeamNumber(UUID captainUUID) {
    return captains.getCaptainTeam(captainUUID);
  }

  private void swapCaptainWithAvailablePlayer(
      Player currentCaptain,
      Player newCaptain,
      String currentCaptainName,
      String newCaptainName,
      int teamNumber) {
    availablePlayers.removePlayer(newCaptainName);
    teams.removePlayerFromTeam(currentCaptainName, teamNumber);

    teams.addPlayerToTeam(newCaptainName, teamNumber);
    teams.assignTeam(newCaptain, teamNumber);

    teams.assignToObserver(currentCaptain);
    availablePlayers.addPlayer(currentCaptainName);
  }
}
