package org.nicolie.towersforpgm.draft.core;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.ConfigManager;
import org.nicolie.towersforpgm.draft.components.DraftDisplayManager;
import org.nicolie.towersforpgm.draft.components.DraftPhase;
import org.nicolie.towersforpgm.draft.components.DraftPickManager;
import org.nicolie.towersforpgm.draft.components.DraftReroll;
import org.nicolie.towersforpgm.draft.components.DraftState;
import org.nicolie.towersforpgm.draft.components.DraftTurnManager;
import org.nicolie.towersforpgm.draft.components.RerollOptionsGUI;
import org.nicolie.towersforpgm.draft.components.SubstituteResult;
import org.nicolie.towersforpgm.draft.events.DraftStartEvent;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.start.StartCountdown;
import tc.oc.pgm.util.bukkit.Sounds;

public class Draft {
  private static Draft instance;

  private final TowersForPGM plugin;
  private final ConfigManager configManager;
  private final DraftState state;
  private final DraftTurnManager turnManager;
  private final DraftPickManager pickManager;
  private final DraftDisplayManager displayManager;
  private final DraftReroll rerollManager;
  private final RerollOptionsGUI rerollOptionsGUI;

  private final Captains captains;
  private final AvailablePlayers availablePlayers;
  private final Teams teams;
  private final Utilities utilities;

  public Draft(
      TowersForPGM plugin,
      ConfigManager configManager,
      DraftState state,
      DraftTurnManager turnManager,
      DraftPickManager pickManager,
      DraftDisplayManager displayManager,
      Captains captains,
      AvailablePlayers availablePlayers,
      Teams teams,
      Utilities utilities) {
    this.plugin = plugin;
    this.configManager = configManager;
    this.state = state;
    this.turnManager = turnManager;
    this.pickManager = pickManager;
    this.displayManager = displayManager;
    this.rerollManager =
        new DraftReroll(plugin, state, captains, availablePlayers, teams, displayManager);
    this.rerollOptionsGUI = new RerollOptionsGUI(plugin, availablePlayers, teams, displayManager);
    this.captains = captains;
    this.availablePlayers = availablePlayers;
    this.teams = teams;
    this.utilities = utilities;
    instance = this;
  }

  public void setCustomOrderPattern(String pattern, int minPlayers) {
    turnManager.setCustomOrderPattern(pattern, minPlayers);
  }

  public void startDraft(
      UUID captain1,
      UUID captain2,
      List<MatchPlayer> players,
      Match match,
      boolean randomizeOrder,
      boolean rerollEnabled) {
    if (MatchManager.getMatch() == null) {
      MatchManager.setCurrentMatch(match);
    }

    initializeDraft(captain1, captain2, players, match, randomizeOrder);

    DraftStartEvent draftStartEvent =
        new DraftStartEvent(captain1, captain2, players, match, randomizeOrder);
    Bukkit.getPluginManager().callEvent(draftStartEvent);

    if (rerollEnabled) {
      state.setCurrentPhase(DraftPhase.CAPTAINS);
      startRerollPhase(match);
    } else {
      startPickingPhase(match);
    }
  }

  private void initializeDraft(
      UUID captain1,
      UUID captain2,
      List<MatchPlayer> players,
      Match match,
      boolean randomizeOrder) {
    pickManager.cleanLists();

    if (!teams.initializeTeamsFromMatch(match)) return;

    captains.setCaptain1(captain1);
    captains.setCaptain2(captain2);
    captains.setCaptain1Turn(randomizeOrder ? new Random().nextBoolean() : true);
    state.setFirstCaptainTurn(captains.isCaptain1Turn());

    teams.removeFromTeams(match);
    teams.addPlayerToTeam(Bukkit.getPlayer(captain1).getName(), 1);
    teams.addPlayerToTeam(Bukkit.getPlayer(captain2).getName(), 2);
    teams.assignTeam(Bukkit.getPlayer(captain1), 1);
    teams.assignTeam(Bukkit.getPlayer(captain2), 2);

    for (MatchPlayer player : players) {
      availablePlayers.addPlayer(player.getNameLegacy());
    }

    match.getCountdown().cancelAll(StartCountdown.class);
    teams.setTeamsSize(0);

    match.playSound(Sounds.RAINDROPS);
    match.sendMessage(Component.text(LanguageManager.message("draft.captains.captainsHeader")));
    match.sendMessage(
        Component.text(teams.getTeamColor(1) + Bukkit.getPlayer(captain1).getName() + " §l§bvs. "
            + teams.getTeamColor(2) + Bukkit.getPlayer(captain2).getName()));
    match.sendMessage(Component.text("§m---------------------------------"));
  }

  private void startPickingPhase(Match match) {
    state.setCurrentPhase(DraftPhase.RUNNING);
    announceCurrentTurn(match);
    plugin.giveItemToMatch(match);
    displayManager.startDraftTimer();
  }

  private void announceCurrentTurn(Match match) {
    int teamNumber = captains.isCaptain1Turn() ? 1 : 2;
    String teamColor = teams.getTeamColor(teamNumber);
    UUID captainUUID = captains.isCaptain1Turn() ? captains.getCaptain1() : captains.getCaptain2();
    String captainName = Bukkit.getPlayer(captainUUID).getName();
    MatchPlayer captainPlayer = MatchManager.getMatch().getPlayer(captainUUID);
    match.sendMessage(Component.text(LanguageManager.message("draft.captains.turn")
        .replace("{teamcolor}", teamColor)
        .replace("{captain}", captainName)));
    DraftDisplayManager.sendTitle(captainPlayer);
  }

  private void startRerollPhase(Match match) {
    rerollManager.startRerollPhase(match, approved -> {
      if (approved) {
        state.setCurrentPhase(DraftPhase.REROLL);
        startCaptainSelectionPhase(match);
      } else {
        startPickingPhase(match);
      }
    });
  }

  private void startCaptainSelectionPhase(Match match) {
    String originalCaptain1 = Bukkit.getPlayer(captains.getCaptain1()).getName();
    String originalCaptain2 = Bukkit.getPlayer(captains.getCaptain2()).getName();

    rerollOptionsGUI.startVoting(match, originalCaptain1, originalCaptain2, selectedPair -> {
      if (selectedPair != null) {
        replaceCaptains(match, selectedPair);
      }

      state.setCurrentPhase(DraftPhase.RUNNING);
      announceCurrentTurn(match);
      plugin.giveItemToMatch(match);
      displayManager.startDraftTimer();
    });
  }

  private void replaceCaptains(Match match, RerollOptionsGUI.CaptainPair selectedPair) {
    Player newCaptain1 = Bukkit.getPlayerExact(selectedPair.captain1);
    Player newCaptain2 = Bukkit.getPlayerExact(selectedPair.captain2);

    if (newCaptain1 == null || newCaptain2 == null) return;

    Player oldCaptain1 = Bukkit.getPlayer(captains.getCaptain1());
    Player oldCaptain2 = Bukkit.getPlayer(captains.getCaptain2());

    returnPlayersToAvailable(oldCaptain1, oldCaptain2);
    resetTeams(match);
    setupNewCaptains(match, newCaptain1, newCaptain2);

    match.sendMessage(Component.text(LanguageManager.message("draft.captains.captainsHeader")));
    match.sendMessage(Component.text(teams.getTeamColor(1) + newCaptain1.getName() + " §l§bvs. "
        + teams.getTeamColor(2) + newCaptain2.getName()));
    match.sendMessage(Component.text("§m---------------------------------"));
  }

  private void returnPlayersToAvailable(Player oldCaptain1, Player oldCaptain2) {
    for (String playerName : teams.getAllTeam(1)) {
      if (!playerName.equals(oldCaptain1.getName())) {
        availablePlayers.addPlayer(playerName);
      }
    }
    for (String playerName : teams.getAllTeam(2)) {
      if (!playerName.equals(oldCaptain2.getName())) {
        availablePlayers.addPlayer(playerName);
      }
    }

    if (oldCaptain1 != null) availablePlayers.addPlayer(oldCaptain1.getName());
    if (oldCaptain2 != null) availablePlayers.addPlayer(oldCaptain2.getName());
  }

  private void resetTeams(Match match) {
    teams.clear();
    teams.initializeTeamsFromMatch(match);
    teams.removeFromTeams(match);
    match.getCountdown().cancelAll(StartCountdown.class);
    captains.setCaptain1Turn(state.isFirstCaptainTurn());
    turnManager.resetTurnOrder();
  }

  private void setupNewCaptains(Match match, Player newCaptain1, Player newCaptain2) {
    captains.setCaptain1(newCaptain1.getUniqueId());
    captains.setCaptain2(newCaptain2.getUniqueId());

    teams.addPlayerToTeam(newCaptain1.getName(), 1);
    teams.addPlayerToTeam(newCaptain2.getName(), 2);
    teams.assignTeam(newCaptain1, 1);
    teams.assignTeam(newCaptain2, 2);
    match.getCountdown().cancelAll(StartCountdown.class);

    availablePlayers.removePlayer(newCaptain1.getName());
    availablePlayers.removePlayer(newCaptain2.getName());
  }

  public void pickPlayer(String username) {
    pickManager.pickPlayer(username);
  }

  public void endDraft() {
    pickManager.endDraft();
  }

  public void cleanLists() {
    displayManager.removeBossbar();
    pickManager.cleanLists();
  }

  public static DraftPhase getPhase() {
    if (instance != null) {
      return instance.state.getCurrentPhase();
    }
    return DraftPhase.IDLE;
  }

  public static boolean isDraftActive() {
    return instance != null && instance.state.getCurrentPhase().isActive();
  }

  public static void showBossBarToPlayer(MatchPlayer matchPlayer) {
    if (instance != null && instance.state.getPickTimerBar() != null) {
      matchPlayer.showBossBar(instance.state.getPickTimerBar());
    }
  }

  public SubstituteResult substituteCaptain(UUID currentCaptainUUID, UUID newCaptainUUID) {
    return pickManager.substituteCaptain(currentCaptainUUID, newCaptainUUID);
  }

  public SubstituteResult substituteCaptainByTeam(int teamNumber, UUID newCaptainUUID) {
    return pickManager.substituteCaptainByTeam(teamNumber, newCaptainUUID);
  }

  public UUID getCaptainByTeam(int teamNumber) {
    if (teamNumber == 1) {
      return captains.getCaptain1();
    } else if (teamNumber == 2) {
      return captains.getCaptain2();
    }
    return null;
  }

  public int getCaptainTeamNumber(UUID captainUUID) {
    return pickManager.getCaptainTeamNumber(captainUUID);
  }

  public AvailablePlayers getAvailablePlayers() {
    return availablePlayers;
  }

  public Teams getTeams() {
    return teams;
  }

  public Captains getCaptains() {
    return captains;
  }

  public DraftReroll getRerollManager() {
    return rerollManager;
  }

  public RerollOptionsGUI getRerollOptionsGUI() {
    return rerollOptionsGUI;
  }

  public DraftState getState() {
    return state;
  }
}
