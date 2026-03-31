package org.nicolie.towersforpgm.session.draft;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.ConfigManager;
import org.nicolie.towersforpgm.draft.events.DraftStartEvent;
import org.nicolie.towersforpgm.draft.events.MatchmakingStartEvent;
import org.nicolie.towersforpgm.draft.map.MapVoteConfig;
import org.nicolie.towersforpgm.draft.map.MapVoteManager;
import org.nicolie.towersforpgm.draft.pick.DraftPickManager;
import org.nicolie.towersforpgm.draft.pick.DraftTurnManager;
import org.nicolie.towersforpgm.draft.pick.gui.PicksGUIManager;
import org.nicolie.towersforpgm.draft.state.*;
import org.nicolie.towersforpgm.draft.team.*;
import org.nicolie.towersforpgm.draft.timer.BossbarTimer;
import org.nicolie.towersforpgm.draft.timer.ReadyReminder;
import org.nicolie.towersforpgm.draft.timer.SuggestionTimer;
import org.nicolie.towersforpgm.session.bridge.CrossMatchBridge;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.start.StartCountdown;
import tc.oc.pgm.util.bukkit.Sounds;
import tc.oc.pgm.util.text.TextFormatter;

public final class DraftContext {

  private final Match match;
  private final TowersForPGM plugin;
  private final ConfigManager config;
  private final AvailablePlayers availablePlayers;
  private final Captains captains;
  private final Teams teams;
  private final DraftState state;

  private final DraftTurnManager turnManager;
  private final DraftPickManager pickManager;
  private final BossbarTimer bossbarTimer;
  private final ReadyReminder readyReminder;
  private final SuggestionTimer suggestionTimer;

  private MapVoteManager mapVoteManager;
  private AssignmentStrategy strategy;

  DraftContext(Match match, TowersForPGM plugin) {
    this.match = match;
    this.plugin = plugin;
    this.config = plugin.config();

    this.availablePlayers = new AvailablePlayers(config);
    this.teams = new Teams(match);
    this.captains = new Captains();
    this.state = new DraftState();

    this.bossbarTimer = new BossbarTimer(match, plugin, config, captains, availablePlayers, teams);
    this.readyReminder = new ReadyReminder(plugin, captains);
    this.suggestionTimer = new SuggestionTimer(plugin, config, captains, availablePlayers);

    this.turnManager = new DraftTurnManager(match, availablePlayers, state, captains, teams);
    this.pickManager = new DraftPickManager(
        match,
        config,
        state,
        captains,
        availablePlayers,
        teams,
        turnManager,
        bossbarTimer,
        suggestionTimer,
        readyReminder);
  }

  void startDraft(
      UUID captain1,
      UUID captain2,
      List<MatchPlayer> players,
      DraftOptions options,
      boolean snapshot) {
    this.strategy = AssignmentStrategy.DRAFT;

    initSharedState(captain1, captain2, players);

    if (!options.getOrderPattern().isEmpty()) {
      turnManager.setCustomOrderPattern(options.getOrderPattern(), options.getMinOrder());
    }

    announceTeams(captain1, captain2);

    if (!snapshot) {
      Bukkit.getPluginManager()
          .callEvent(
              new DraftStartEvent(captain1, captain2, players, match, options.isRandomizeOrder()));
    }

    MapVoteConfig mapCfg = options.getMapVoteConfig() != null
        ? options.getMapVoteConfig()
        : MapVoteConfig.builder().maps(List.of()).build();

    this.mapVoteManager =
        new MapVoteManager(match, plugin, mapCfg, captains, availablePlayers, bossbarTimer);

    if (options.hasMapVote()) {
      startMapVotePhase();
    } else {
      startPickingPhase();
    }
  }

  void startMatchmaking(UUID captain1, UUID captain2, List<MatchPlayer> players) {
    this.strategy = AssignmentStrategy.MATCHMAKING;

    initSharedState(captain1, captain2, players);
    announceTeams(captain1, captain2);

    Bukkit.getPluginManager()
        .callEvent(new MatchmakingStartEvent(captain1, captain2, players, match));

    MatchmakingAssigner.assign(this);
  }

  public void pickPlayer(String username) {
    requireDraft("pickPlayer");
    pickManager.pickPlayer(username);
  }

  public boolean suggestPlayer(MatchPlayer advisor, String username) {
    requireDraft("suggestPlayer");
    return pickManager.suggestPlayer(advisor, username);
  }

  public void activatePlayerSuggestions(MatchPlayer captain) {
    requireDraft("activatePlayerSuggestions");

    if (phase() != DraftPhase.RUNNING) {
      captain.sendWarning(Component.translatable("draft.suggestions.notActive"));
      return;
    }

    if (captains.isPlayerSuggestions()) return;

    captains.setPlayerSuggestions(true);

    Party team = captain.getParty();
    Title suggestion = Title.title(
        Component.translatable("draft.suggestions.title"),
        Component.translatable("draft.suggestions.subtitle"),
        Title.Times.times(
            java.time.Duration.ofMillis(500),
            java.time.Duration.ofSeconds(5),
            java.time.Duration.ofMillis(500)));

    team.showTitle(suggestion);
    team.playSound(Sounds.TIP);

    captain.clearTitle();
    captain.sendMessage(
        Component.translatable("draft.suggestions.activated").color(NamedTextColor.GRAY));
  }

  public void addToPool(String username) {
    availablePlayers().addPlayer(username);
    match.sendMessage(Component.translatable("draft.add", MatchManager.getPrefixedName(username))
        .color(NamedTextColor.GRAY));
    match.playSound(Sounds.ALERT);
  }

  public void removeFromPool(String username) {
    availablePlayers.removePlayer(username);
    if (availablePlayers.getAllAvailablePlayers().isEmpty()) {
      pickManager.endDraft();
    }
  }

  public PickResult validatePick(UUID captainUUID, String targetName) {
    if (state.getCurrentPhase() != DraftPhase.RUNNING) return PickResult.DRAFT_NOT_ACTIVE;

    int captainNumber = getCaptainNumber(captainUUID);
    if (captainNumber == -1) return PickResult.NOT_A_CAPTAIN;

    boolean myTurn = (captainNumber == 1 && captains.isCaptain1Turn())
        || (captainNumber == 2 && !captains.isCaptain1Turn());

    if (!myTurn) return PickResult.NOT_YOUR_TURN;
    if (!availablePlayers.isPlayerAvailable(targetName)) return PickResult.NOT_IN_DRAFT;
    if (teams.isPlayerInAnyTeam(targetName)) return PickResult.ALREADY_PICKED;

    return PickResult.OK;
  }

  public ReadyResult validateReady(UUID captainUUID) {
    if (!captains.isReadyActive()) return ReadyResult.NOT_AVAILABLE;

    int captainNumber = getCaptainNumber(captainUUID);
    if (captainNumber == -1) return ReadyResult.NOT_A_CAPTAIN;

    boolean alreadyReady = captainNumber == 1 ? captains.isReady1() : captains.isReady2();
    if (alreadyReady) return ReadyResult.ALREADY_READY;

    return ReadyResult.OK;
  }

  public AddResult validateAdd(String name) {
    if (captains.isCaptainByName(name)) return AddResult.IS_CAPTAIN;
    if (teams.isPlayerInAnyTeam(name)) return AddResult.ALREADY_PICKED;
    if (availablePlayers.isPlayerAvailable(name)) return AddResult.ALREADY_IN_DRAFT;
    return AddResult.OK;
  }

  public RemoveResult validateRemove(String name) {
    if (!availablePlayers.isPlayerAvailable(name)) return RemoveResult.NOT_IN_DRAFT;
    if (teams.isPlayerInAnyTeam(name)) return RemoveResult.ALREADY_PICKED;
    return RemoveResult.OK;
  }

  public DraftPhase phase() {
    return state.getCurrentPhase();
  }

  public boolean isActive() {
    return state.getCurrentPhase().isActive();
  }

  public AssignmentStrategy strategy() {
    return strategy;
  }

  public Match match() {
    return match;
  }

  public AvailablePlayers availablePlayers() {
    return availablePlayers;
  }

  public Captains captains() {
    return captains;
  }

  public Teams teams() {
    return teams;
  }

  public DraftState state() {
    return state;
  }

  public ReadyReminder readyReminder() {
    return readyReminder;
  }

  public MapVoteManager mapVoteManager() {
    return mapVoteManager;
  }

  public Component getOrderStyled() {
    return turnManager.getOrderStyled();
  }

  public String getOrderPattern() {
    return state.getCustomOrderPattern();
  }

  public int getPatternIndex() {
    return state.getCurrentPatternIndex();
  }

  public boolean isUsingCustomPattern() {
    return state.isUsingCustomPattern();
  }

  public int getOrderMinPlayers() {
    return state.getCustomOrderMinPlayers();
  }

  public void resumePickTimer() {
    pickManager.startTurnTimer();
  }

  public void finalizeTeams() {
    pickManager.finalizeTeams();
  }

  public void markReady(UUID captainUUID, Match match) {
    int captainNumber = getCaptainNumber(captainUUID);
    if (captainNumber == 1) readyReminder.setReady(1, match);
    else if (captainNumber == 2) readyReminder.setReady(2, match);
  }

  public void showBossBarTo(MatchPlayer player) {
    if (state.getPickTimerBar() != null) player.showBossBar(state.getPickTimerBar());
  }

  public SubstituteResult substituteCaptainByTeam(int teamNumber, UUID newCaptainUUID) {
    return pickManager.substituteCaptainByTeam(teamNumber, newCaptainUUID);
  }

  public UUID getCaptainByTeam(int teamNumber) {
    return teamNumber == 1
        ? captains.getCaptain1()
        : teamNumber == 2 ? captains.getCaptain2() : null;
  }

  public int getCaptainNumber(UUID uuid) {
    if (uuid == null) return -1;
    if (uuid.equals(captains.getCaptain1())) return 1;
    if (uuid.equals(captains.getCaptain2())) return 2;
    return -1;
  }

  public int getCaptainNumber(String username) {
    var player = Bukkit.getPlayerExact(username);
    return player != null ? getCaptainNumber(player.getUniqueId()) : -1;
  }

  public Component captainName(int teamNumber) {
    return bossbarTimer.captainName(teamNumber);
  }

  void cancelReadyReminder() {
    readyReminder.cancelTimer();
  }

  void cleanup() {
    if (state.getDraftTimer() != null) state.getDraftTimer().cancel();
    if (bossbarTimer != null) bossbarTimer.cancelTimer();
    if (suggestionTimer != null) suggestionTimer.cancelTimer();
    if (readyReminder != null) readyReminder.cancelTimer();
    if (mapVoteManager != null) mapVoteManager.cancel();

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

  private void initSharedState(UUID captain1, UUID captain2, List<MatchPlayer> players) {
    cleanup();

    if (!teams.initializeTeamsFromMatch(match)) return;

    captains.setCaptain1(captain1);
    captains.setCaptain2(captain2);
    captains.setCaptain1Turn(new Random().nextBoolean());
    state.setFirstCaptainTurn(captains.isCaptain1Turn());

    teams.removeFromTeams(match);

    var p1 = Bukkit.getPlayer(captain1);
    var p2 = Bukkit.getPlayer(captain2);

    teams.addPlayerToTeam(p1.getName(), 1);
    teams.addPlayerToTeam(p2.getName(), 2);
    teams.assignTeam(p1, 1);
    teams.assignTeam(p2, 2);

    for (MatchPlayer p : players) {
      availablePlayers.addPlayer(p.getNameLegacy());
    }

    match.getCountdown().cancelAll(StartCountdown.class);
    teams.setTeamsSize(0);
    match.playSound(Sounds.RAINDROPS);
  }

  private void startPickingPhase() {
    state.setCurrentPhase(DraftPhase.RUNNING);
    announceCurrentTurn();
    PicksGUIManager.giveItem(match);
    pickManager.startTurnTimer();
  }

  private void startMapVotePhase() {
    state.setCurrentPhase(DraftPhase.MAP);

    mapVoteManager.startVote(winningMap -> {
      state.setCurrentPhase(DraftPhase.RUNNING);
      CrossMatchBridge.getInstance().capture(match);
      match.sendMessage(Component.translatable(
              "draft.map.selected", Component.text(winningMap).color(NamedTextColor.GOLD))
          .color(NamedTextColor.AQUA));
      MapInfo info = MatchManager.getMatchInfo(winningMap);
      MatchManager.setNextMap(match, info);
    });
  }

  private void announceTeams(UUID c1, UUID c2) {
    Component header =
        Component.translatable("draft.captains.captainsHeader").color(NamedTextColor.AQUA);

    for (MatchPlayer viewer : match.getPlayers()) {
      viewer.sendMessage(TextFormatter.horizontalLineHeading(
          viewer.getBukkit(), header, NamedTextColor.WHITE, 200));

      viewer.sendMessage(
          Component.text(teams.getTeamColor(1) + Bukkit.getPlayer(c1).getName()
              + " §l§bvs. "
              + teams.getTeamColor(2) + Bukkit.getPlayer(c2).getName()));

      viewer.sendMessage(TextFormatter.horizontalLine(NamedTextColor.WHITE, 200));
    }
  }

  private void announceCurrentTurn() {
    int team = captains.isCaptain1Turn() ? 1 : 2;
    UUID uuid = captains.getCurrentCaptain();

    MatchPlayer captain = match.getPlayer(uuid);
    if (captain != null) DraftPickManager.sendTitle(captain);

    match.sendMessage(Component.translatable(
            "draft.turn",
            Component.text(
                    Bukkit.getPlayer(uuid) != null
                        ? Bukkit.getPlayer(uuid).getName()
                        : teams.getTeamName(team))
                .color(teams.getTeam(team).getTextColor()))
        .color(NamedTextColor.GRAY));
  }

  private void requireDraft(String methodName) {
    if (strategy != AssignmentStrategy.DRAFT) {
      throw new IllegalStateException(methodName + "() solo válido en DRAFT, actual: " + strategy);
    }
  }
}
