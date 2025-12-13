package org.nicolie.towersforpgm.draft.core;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.ConfigManager;
import org.nicolie.towersforpgm.draft.components.DraftDisplayManager;
import org.nicolie.towersforpgm.draft.components.DraftPickManager;
import org.nicolie.towersforpgm.draft.components.DraftState;
import org.nicolie.towersforpgm.draft.components.DraftTurnManager;
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
      boolean randomizeOrder) {
    if (MatchManager.getMatch() == null) {
      MatchManager.setCurrentMatch(match);
    }
    pickManager.cleanLists();

    if (!teams.initializeTeamsFromMatch(match)) {
      return;
    }

    state.setDraftActive(true);
    captains.setCaptain1Turn(true);
    captains.setCaptain1(captain1);
    captains.setCaptain2(captain2);

    teams.removeFromTeams(match);

    teams.addPlayerToTeam(Bukkit.getPlayer(captain1).getName(), 1);
    teams.addPlayerToTeam(Bukkit.getPlayer(captain2).getName(), 2);

    for (MatchPlayer player : players) {
      availablePlayers.addPlayer(player.getNameLegacy());
    }
    teams.assignTeam(Bukkit.getPlayer(captain1), 1);
    teams.assignTeam(Bukkit.getPlayer(captain2), 2);

    match.getCountdown().cancelAll(StartCountdown.class);
    teams.setTeamsSize(0);

    if (randomizeOrder) {
      Random random = new Random();
      captains.setCaptain1Turn(random.nextBoolean());
    }

    state.setFirstCaptainTurn(captains.isCaptain1Turn());

    match.playSound(Sounds.RAINDROPS);
    match.sendMessage(Component.text(LanguageManager.message("draft.captains.captainsHeader")));
    match.sendMessage(
        Component.text(teams.getTeamColor(1) + Bukkit.getPlayer(captain1).getName() + " §l§bvs. "
            + teams.getTeamColor(2) + Bukkit.getPlayer(captain2).getName()));
    match.sendMessage(Component.text("§m---------------------------------"));

    int currentTeamNumber = captains.isCaptain1Turn() ? 1 : 2;
    String teamColor = teams.getTeamColor(currentTeamNumber);
    UUID captainUUID = captains.isCaptain1Turn() ? captains.getCaptain1() : captains.getCaptain2();
    String captainName = Bukkit.getPlayer(captainUUID).getName();
    match.sendMessage(Component.text(LanguageManager.message("draft.captains.turn")
        .replace("{teamcolor}", teamColor)
        .replace("{captain}", captainName)));
    plugin.giveitem(match.getWorld());

    DraftStartEvent draftStartEvent =
        new DraftStartEvent(captain1, captain2, players, match, randomizeOrder);
    Bukkit.getPluginManager().callEvent(draftStartEvent);

    displayManager.startDraftTimer();
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

  public static boolean isDraftActive() {
    return instance != null && instance.state.isDraftActive();
  }

  public static void showBossBarToPlayer(MatchPlayer matchPlayer) {
    if (instance != null && instance.state.getPickTimerBar() != null) {
      matchPlayer.showBossBar(instance.state.getPickTimerBar());
    }
  }

  public SubstituteResult substituteCaptain(UUID currentCaptainUUID, UUID newCaptainUUID) {
    return pickManager.substituteCaptain(currentCaptainUUID, newCaptainUUID);
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
}
