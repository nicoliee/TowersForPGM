package org.nicolie.towersforpgm.draft.components;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.ConfigManager;
import org.nicolie.towersforpgm.draft.core.AvailablePlayers;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.draft.core.Utilities;
import org.nicolie.towersforpgm.draft.events.DraftEndEvent;
import org.nicolie.towersforpgm.draft.picksMenu.PicksGUIManager;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.util.bukkit.Sounds;
import tc.oc.pgm.util.text.TextFormatter;

public class DraftPickManager {
  private final TowersForPGM plugin;
  private final ConfigManager configManager;
  private final DraftState state;
  private final Captains captains;
  private final AvailablePlayers availablePlayers;
  private final Teams teams;
  private final Utilities utilities;
  private final DraftTurnManager turnManager;
  private BossbarTimer displayManager;

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

  public void setDisplayManager(BossbarTimer displayManager) {
    this.displayManager = displayManager;
  }

  public void pickPlayer(String username) {
    Player player = Bukkit.getPlayerExact(username);
    String exactUsername = availablePlayers.getExactUser(username);
    int teamNumber = captains.isCaptain1Turn() ? 1 : 2;
    Sound sound = captains.isCaptain1Turn() ? Sounds.MATCH_COUNTDOWN : Sounds.MATCH_START;

    assignPlayerToTeam(player, exactUsername, teamNumber);
    broadcastPlayerPick(teamNumber, exactUsername);
    MatchManager.getMatch().playSound(sound);
    MatchPlayer currentCaptain = PGM.get()
        .getMatchManager()
        .getPlayer(captains.isCaptain1Turn() ? captains.getCaptain1() : captains.getCaptain2());
    if (currentCaptain != null) {
      currentCaptain.clearTitle();
    }
    turnManager.updateTurnOrder();

    if (handleSecondPickBalance()) {
      return;
    }

    if (handleLastPick()) {
      return;
    }

    if (availablePlayers.isEmpty()) {
      endDraft();
      return;
    }
    MatchPlayer newCaptain = PGM.get()
        .getMatchManager()
        .getPlayer(captains.isCaptain1Turn() ? captains.getCaptain1() : captains.getCaptain2());
    if (newCaptain != null) {
      sendTitle(newCaptain);
    }
    if (displayManager != null) {
      displayManager.startDraftTimer();
    }
  }

  public boolean suggestPlayer(MatchPlayer advisor, String suggestion) {
    if (availablePlayers.hasAlreadySuggested(advisor.getNameLegacy())
        || availablePlayers.isPlayerAvailable(advisor.getNameLegacy())
        || suggestion == null
        || !availablePlayers.isPlayerAvailable(suggestion)) {
      return false;
    }

    if (!captains.isPlayerSuggestions()) {
      return false;
    }

    int currentCaptain = captains.isCaptain1Turn() ? 1 : 2;
    int advisorTeam = teams.getTeamNumber(advisor.getNameLegacy());

    if (advisorTeam != currentCaptain) {
      return false;
    }

    MatchPlayer captain = PGM.get()
        .getMatchManager()
        .getPlayer(captains.isCaptain1Turn() ? captains.getCaptain1() : captains.getCaptain2());
    captain
        .getParty()
        .sendMessage(Component.translatable(
            "draft.suggestions.suggest",
            advisor.getName(),
            MatchManager.getPrefixedName(suggestion)));

    captain.getParty().playSound(Sounds.ITEM_PICKUP);
    availablePlayers.recordSuggestion(advisor.getNameLegacy());
    return true;
  }

  public void endDraft() {
    if (state.getDraftTimer() != null) {
      state.getDraftTimer().cancel();
    }

    if (!state.getCurrentPhase().isActive()) {
      return;
    }

    // Transition to ENDED phase
    state.setCurrentPhase(DraftPhase.ENDED);

    if (state.getDraftTimer() != null) {
      state.getDraftTimer().cancel();
    }

    List<String> team1Names = new ArrayList<>(teams.getAllTeam(1));
    List<String> team2Names = new ArrayList<>(teams.getAllTeam(2));

    Component team1 = utilities.buildLists(team1Names, teams.getTeam(1).getTextColor(), false);
    Component team2 = utilities.buildLists(team2Names, teams.getTeam(2).getTextColor(), false);
    int team1Size = team1Names.size();
    int team2Size = team2Names.size();
    int teamsize = Math.max(team1Size, team2Size);
    Match match = MatchManager.getMatch();
    Component teamsMessage =
        Component.translatable("draft.captains.teamsHeader").color(NamedTextColor.AQUA);
    for (MatchPlayer viewer : match.getPlayers()) {
      viewer.sendMessage(TextFormatter.horizontalLineHeading(
          viewer.getBukkit(), teamsMessage, NamedTextColor.WHITE, 200));
      viewer.sendMessage(team1);
      viewer.sendMessage(Component.text("§8[" + teams.getTeamColor(1).replace("&", "§") + team1Size
          + "§8] §l§bvs. " + "§8[" + teams.getTeamColor(2).replace("&", "§") + team2Size + "§8]"));
      viewer.sendMessage(team2);
      viewer.sendMessage(TextFormatter.horizontalLine(NamedTextColor.WHITE, 200));
    }

    teams.setTeamsSize(teamsize);

    captains.setReadyActive(true);
    captains.setMatchWithCaptains(true);

    PicksGUIManager.removeItem(MatchManager.getMatch());
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
    availablePlayers.clearSuggestions();
    teams.clear();
    state.setCurrentPhase(DraftPhase.IDLE);
    if (state.getDraftTimer() != null) {
      state.getDraftTimer().cancel();
      state.setDraftTimer(null);
    }
  }

  public SubstituteResult substituteCaptain(UUID currentCaptainUUID, UUID newCaptainUUID) {
    if (!state.getCurrentPhase().isActive() || !captains.isCaptain(currentCaptainUUID)) {
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
      PicksGUIManager.giveItem(MatchManager.getMatch());
    }

    if (displayManager != null) {
      displayManager.startDraftTimer();
    }

    return SubstituteResult.SUCCESS;
  }

  public SubstituteResult substituteCaptainByTeam(int teamNumber, UUID newCaptainUUID) {
    if (!state.getCurrentPhase().isActive() || teamNumber < 1 || teamNumber > 2) {
      return SubstituteResult.NOT_CAPTAIN;
    }

    // Obtener el UUID del capitán actual del equipo
    UUID currentCaptainUUID = teamNumber == 1 ? captains.getCaptain1() : captains.getCaptain2();
    if (currentCaptainUUID == null) {
      return SubstituteResult.NOT_CAPTAIN;
    }

    Player newCaptain = Bukkit.getPlayer(newCaptainUUID);
    if (newCaptain == null) {
      return SubstituteResult.NOT_AVAILABLE;
    }

    String newCaptainName = newCaptain.getName();

    // El capitán actual puede estar conectado o desconectado
    Player currentCaptain = Bukkit.getPlayer(currentCaptainUUID);
    String currentCaptainName = currentCaptain != null
        ? currentCaptain.getName()
        : Bukkit.getOfflinePlayer(currentCaptainUUID).getName();

    // Verificar que el nuevo capitán no esté en el equipo enemigo
    if (teams.isPlayerInTeam(newCaptainName, teamNumber == 1 ? 2 : 1)) {
      return SubstituteResult.ENEMY_TEAM;
    }

    // Verificar que el nuevo capitán esté disponible o en el mismo equipo
    if (!availablePlayers.isPlayerAvailable(newCaptainName)
        && !teams.isPlayerInTeam(newCaptainName, teamNumber)) {
      return SubstituteResult.NOT_AVAILABLE;
    }

    // Si el nuevo capitán está disponible, intercambiarlo con el capitán actual
    if (availablePlayers.isPlayerAvailable(newCaptainName)) {
      availablePlayers.removePlayer(newCaptainName);
      teams.removePlayerFromTeam(currentCaptainName, teamNumber);

      teams.addPlayerToTeam(newCaptainName, teamNumber);
      teams.assignTeam(newCaptain, teamNumber);

      if (currentCaptain != null) {
        teams.assignToObserver(currentCaptain);
      }
      availablePlayers.addPlayer(currentCaptainName);
    }

    // Sustituir el capitán
    captains.substituteCaptain(currentCaptainUUID, newCaptainUUID);

    // Reiniciar el timer
    if (displayManager != null) {
      displayManager.startDraftTimer();
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

  private boolean handleSecondPickBalance() {
    if (!configManager.draft().isSecondPickBalance()) {
      return false;
    }

    if (availablePlayers.getAllAvailablePlayers().size() != 2) {
      return false;
    }

    Set<String> team1List = teams.getAllTeam(1);
    Set<String> team2List = teams.getAllTeam(2);
    int totalPlayers = team1List.size()
        + team2List.size()
        + availablePlayers.getAllAvailablePlayers().size();

    if (totalPlayers % 2 == 0) {
      return false;
    }

    List<String> lastTwoPlayers = new ArrayList<>(availablePlayers.getAllAvailablePlayers());
    int team1Size = team1List.size();
    int team2Size = team2List.size();
    int teamToAssign = team1Size < team2Size ? 1 : 2;

    for (String p : lastTwoPlayers) {
      Player pl = Bukkit.getPlayerExact(p);
      String exactName = availablePlayers.getExactUser(p);
      assignPlayerToTeam(pl, exactName, teamToAssign);
      broadcastPlayerPick(teamToAssign, exactName);
    }
    finalizeDraft();
    return true;
  }

  private boolean handleLastPick() {
    if (availablePlayers.getAllAvailablePlayers().size() != 1) {
      return false;
    }

    String lastPlayer = availablePlayers.getAllAvailablePlayers().iterator().next();
    int teamNumber = captains.isCaptain1Turn() ? 1 : 2;
    Player pl = Bukkit.getPlayerExact(lastPlayer);
    String exactName = availablePlayers.getExactUser(lastPlayer);
    assignPlayerToTeam(pl, exactName, teamNumber);
    broadcastPlayerPick(teamNumber, exactName);
    finalizeDraft();
    return true;
  }

  private void assignPlayerToTeam(Player player, String exactName, int teamNumber) {
    teams.addPlayerToTeam(exactName, teamNumber);
    availablePlayers.removePlayer(exactName);
    availablePlayers.recordPick(exactName, teamNumber);
    if (player != null) {
      teams.assignTeam(player, teamNumber);
      PicksGUIManager.giveItem(player);
    }
  }

  private void broadcastPlayerPick(int teamNumber, String playerName) {
    Match match = MatchManager.getMatch();
    Component captainName = teamNumber == 1
        ? MatchManager.getPrefixedName(captains.getCaptain1Name())
        : MatchManager.getPrefixedName(captains.getCaptain2Name());
    if (captainName == null) {
      captainName = teams.getTeam(teamNumber).getName();
    }
    match.sendMessage(Component.translatable(
        "draft.pick", captainName, MatchManager.getPrefixedName(playerName)));
  }

  private void finalizeDraft() {
    MatchManager.getMatch().playSound(Sounds.MATCH_START);
    endDraft();
  }

  public static void sendTitle(MatchPlayer player) {
    Title title = Title.title(
        Component.translatable("draft.captains.title")
            .color(NamedTextColor.WHITE)
            .decorate(TextDecoration.BOLD),
        Component.translatable("draft.captains.subtitle"),
        Title.Times.times(
            java.time.Duration.ofMillis(500),
            java.time.Duration.ofMillis(3000),
            java.time.Duration.ofMillis(500)));
    player.showTitle(title);
  }
}
