package org.nicolie.towersforpgm.draft;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.events.DraftEndEvent;
import org.nicolie.towersforpgm.draft.events.DraftStartEvent;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.MatchManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.start.StartCountdown;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.util.bukkit.Sounds;

public class Draft {
  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final Captains captains;
  private final AvailablePlayers availablePlayers;
  private final Teams teams;
  private final Utilities utilities;
  private static boolean isDraftActive = false;
  private BukkitRunnable draftTimer;
  private static BossBar pickTimerBar;

  // Custom draft order pattern variables
  private String customOrderPattern = ""; // Ejemplo: "ABABAB"
  // 'A' significa turno del capitán que fue primero, 'B' significa turno del otro capitán
  private int customOrderMinPlayers = 6; // Número mínimo de jugadores para usar el patrón
  private int currentPatternIndex = 0; // Índice actual del patrón
  private boolean usingCustomPattern = false; // Indica si se está usando un patrón personalizado
  private boolean firstCaptainTurn; // Indica quién fue el primer capitán en pickear

  public Draft(
      Captains captains, AvailablePlayers availablePlayers, Teams teams, Utilities utilities) {
    this.teams = teams;
    this.captains = captains;
    this.availablePlayers = availablePlayers;
    this.utilities = utilities;
  }

  public void setCustomOrderPattern(String pattern, int minPlayers) {
    if (pattern != null && !pattern.isEmpty()) {
      this.customOrderPattern = pattern.toUpperCase();
      this.customOrderMinPlayers = minPlayers;
    }
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
    cleanLists();

    // Initialize teams from match
    if (!teams.initializeTeamsFromMatch(match)) {
      return; // Failed to initialize teams
    }

    isDraftActive = true;
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

    firstCaptainTurn = captains.isCaptain1Turn();

    if (!customOrderPattern.isEmpty()
        && availablePlayers.getAllAvailablePlayers().size() >= customOrderMinPlayers) {
      usingCustomPattern = true;
      currentPatternIndex = 0;
    } else {
      usingCustomPattern = false;
      customOrderPattern = "";
      currentPatternIndex = 0;
    }

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

    // Disparar evento de inicio de draft
    DraftStartEvent draftStartEvent =
        new DraftStartEvent(captain1, captain2, players, match, randomizeOrder);
    Bukkit.getPluginManager().callEvent(draftStartEvent);

    startDraftTimer();
  }

  public void startDraftTimer() {
    if (!plugin.config().draft().isDraftTimer()) {
      return;
    }

    if (pickTimerBar == null) {
      String currentCaptainName =
          captains.isCaptain1Turn() ? captains.getCaptain1Name() : captains.getCaptain2Name();
      int currentTeamNumber = captains.isCaptain1Turn() ? 1 : 2;
      String currentCaptainColor = teams.getTeamColor(currentTeamNumber);
      String bossbarMessage = LanguageManager.message("draft.captains.bossbar")
          .replace("{captain}", currentCaptainColor + currentCaptainName);

      pickTimerBar = BossBar.bossBar(
          Component.text(
              bossbarMessage.replace("{time}", String.valueOf(utilities.timerDuration()))),
          1f,
          BossBar.Color.YELLOW,
          BossBar.Overlay.PROGRESS);
      MatchManager.getMatch().showBossBar(pickTimerBar);
    }

    if (draftTimer != null) {
      draftTimer.cancel();
    }

    int initialTime = utilities.timerDuration();
    int[] timeLeft = {initialTime};

    MatchPlayer currentCaptain =
        PGM.get().getMatchManager().getPlayer(captains.getCurrentCaptain());
    String currentCaptainName;
    if (currentCaptain != null) {
      currentCaptainName =
          captains.isCaptain1Turn() ? captains.getCaptain1Name() : captains.getCaptain2Name();
    } else {
      int teamNumber = captains.isCaptain1Turn() ? 1 : 2;
      currentCaptainName = teams.getTeamName(teamNumber);
    }
    int currentTeamNumber = captains.isCaptain1Turn() ? 1 : 2;
    String currentCaptainColor = teams.getTeamColor(currentTeamNumber);
    String bossbarMessage = LanguageManager.message("draft.captains.bossbar")
        .replace("{captain}", currentCaptainColor + currentCaptainName);
    if (currentCaptain != null) {
      currentCaptain.sendActionBar(Component.text(LanguageManager.message("draft.captains.tip")));
    }
    draftTimer = new BukkitRunnable() {
      @Override
      public void run() {
        float progress = Math.max(0f, (float) timeLeft[0] / initialTime);
        pickTimerBar.name(
            Component.text(bossbarMessage.replace("{time}", SendMessage.formatTime(timeLeft[0]))));
        pickTimerBar.progress(progress);

        if (timeLeft[0] == initialTime - 5) {
          utilities.suggestPicksForCaptains();
        }
        if (timeLeft[0] <= 30 && timeLeft[0] > 3) {
          MatchPlayer currentCaptain =
              PGM.get().getMatchManager().getPlayer(captains.getCurrentCaptain());
          if (currentCaptain != null) {
            currentCaptain.playSound(Sounds.INVENTORY_CLICK);
          }
        } else if (timeLeft[0] <= 3 && timeLeft[0] >= 1) {
          MatchPlayer currentCaptain =
              PGM.get().getMatchManager().getPlayer(captains.getCurrentCaptain());
          if (currentCaptain != null) {
            currentCaptain.playSound(Sounds.WARNING);
          }
        }
        if ((timeLeft[0] <= 5 && timeLeft[0] >= 1)) {
          MatchPlayer currentCaptain =
              PGM.get().getMatchManager().getPlayer(captains.getCurrentCaptain());
          if (currentCaptain != null) {
            SendMessage.sendToPlayer(
                currentCaptain.getBukkit(),
                LanguageManager.message("draft.captains.timeRemaining")
                    .replace("{time}", SendMessage.formatTime(timeLeft[0])));
          }
        }

        if (timeLeft[0] == 0) {
          if (utilities.randomPick() == null) {
            endDraft();
          } else {
            pickPlayer(utilities.randomPick());
            this.cancel();
          }
        }
        timeLeft[0]--;
      }
    };
    draftTimer.runTaskTimer(TowersForPGM.getInstance(), 0, 20); // cada segundo
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

    updateTurnOrder();

    if (plugin.config().draft().isSecondPickBalance()) {
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
    startDraftTimer();
  }

  private void updateTurnOrder() {
    if (usingCustomPattern) {
      currentPatternIndex++;

      // Si hemos llegado al final del patrón, alternamos el turno
      if (currentPatternIndex >= customOrderPattern.length()) {
        usingCustomPattern = false;
        captains.toggleTurn();
      } else {
        // Obtener el siguiente capitán según el patrón
        char nextCaptain = customOrderPattern.charAt(currentPatternIndex);
        boolean shouldBeCaptain1Turn;

        if (nextCaptain == 'A') {
          // Si es 'A', debe ser el turno del capitán que pickeo primero
          shouldBeCaptain1Turn = firstCaptainTurn;
        } else {
          // Si es 'B', debe ser el turno del otro capitán
          shouldBeCaptain1Turn = !firstCaptainTurn;
        }
        // Si un capitán debe pickear seguido enviar un mensaje
        if ((shouldBeCaptain1Turn == captains.isCaptain1Turn())
            || (!shouldBeCaptain1Turn == !captains.isCaptain1Turn())) {
          int currentTeamNumber = captains.isCaptain1Turn() ? 1 : 2;
          String message = LanguageManager.message("draft.captains.turn")
              .replace("{teamcolor}", teams.getTeamColor(currentTeamNumber).replace("§", "&"))
              .replace(
                  "{captain}",
                  Bukkit.getPlayer(
                          captains.isCaptain1Turn()
                              ? captains.getCaptain1()
                              : captains.getCaptain2())
                      .getName());
          SendMessage.broadcast(message);
        }
        captains.setCaptain1Turn(shouldBeCaptain1Turn);
      }
    } else {
      captains.toggleTurn();
    }
  }

  public void endDraft() {
    if (draftTimer != null) {
      draftTimer.cancel();
    }

    if (!isDraftActive) {
      return;
    }

    isDraftActive = false;

    // cancelar timer
    if (draftTimer != null) {
      draftTimer.cancel();
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

    removeBossbar();

    // Disparar evento de fin de draft
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
    isDraftActive = false;
    usingCustomPattern = false;
    currentPatternIndex = 0;
    if (draftTimer != null) {
      draftTimer.cancel();
    }
  }

  private void removeBossbar() {
    if (pickTimerBar != null) {
      MatchManager.getMatch().hideBossBar(pickTimerBar);
      pickTimerBar = null;
    }
  }

  public static boolean isDraftActive() {
    return isDraftActive;
  }

  public static void showBossBarToPlayer(MatchPlayer matchPlayer) {
    if (pickTimerBar != null) {
      matchPlayer.showBossBar(pickTimerBar);
    }
  }

  public SubstituteResult substituteCaptain(UUID currentCaptainUUID, UUID newCaptainUUID) {
    if (!isDraftActive || !captains.isCaptain(currentCaptainUUID)) {
      return SubstituteResult.NOT_CAPTAIN;
    }

    // Obtener jugadores
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

    // Dar item al nuevo capitán si es su turno
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
