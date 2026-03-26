package org.nicolie.towersforpgm.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.state.DraftPhase;
import org.nicolie.towersforpgm.draft.team.AvailablePlayers;
import org.nicolie.towersforpgm.draft.team.Captains;
import org.nicolie.towersforpgm.draft.team.Teams;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.stats.StatsMatchModule;
import tc.oc.pgm.util.bukkit.Sounds;

public class CommandListener implements Listener {

  private static final List<String> MATCH_ENDING_COMMANDS =
      Arrays.asList("/end", "/cycle", "/qr", "/finish");
  private static final String FORCE_FLAG = " -f";
  private static final String TEAM_COMMAND = "/team";
  private static final String MATCH_COMMAND = "/match";

  @EventHandler
  public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
    String command = event.getMessage();
    String lowerCommand = command.toLowerCase();
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());

    if (lowerCommand.startsWith(MATCH_COMMAND)) {
      handleMatchCommand(player);
      return;
    }

    Match match = player.getMatch();
    boolean isRanked = Queue.isRanked();
    boolean isDraft = isDraftActive(match);

    if (isProtectedCommand(lowerCommand) && (isRanked || isDraft) && match.isRunning()) {
      handleProtectedCommand(event, command, lowerCommand, player, isRanked);
    } else if (lowerCommand.startsWith(TEAM_COMMAND) && isRanked) {
      handleTeamCommand(event, command, player);
    }
  }

  private boolean isProtectedCommand(String command) {
    return MATCH_ENDING_COMMANDS.stream().anyMatch(command::startsWith);
  }

  private void handleProtectedCommand(
      PlayerCommandPreprocessEvent event,
      String command,
      String lowerCommand,
      MatchPlayer player,
      boolean isRanked) {
    if (lowerCommand.endsWith(FORCE_FLAG)) {
      event.setMessage(removeForceFlag(command));
    } else {
      event.setCancelled(true);
      String key =
          isRanked ? "ranked.commands.protectedInRanked" : "draft.commands.protectedInDraft";
      player.sendWarning(Component.translatable(key));
    }
  }

  private void handleTeamCommand(
      PlayerCommandPreprocessEvent event, String command, MatchPlayer player) {
    if (command.toLowerCase().endsWith(FORCE_FLAG)) {
      event.setMessage(removeForceFlag(command));
    } else {
      event.setCancelled(true);
      player.sendWarning(Component.translatable("ranked.commands.teamChangeBlocked"));
    }
  }

  private void handleMatchCommand(MatchPlayer player) {
    Match match = player.getMatch();

    displayDraftInfo(player);

    ScoreMatchModule scoreModule = match.getModule(ScoreMatchModule.class);
    StatsMatchModule statsModule = match.getModule(StatsMatchModule.class);

    if (!isValidForScoreDisplay(match, scoreModule, statsModule)) return;

    List<MatchPlayer> allPlayers = getAllMatchPlayers(match);
    List<Map.Entry<MatchPlayer, Integer>> playerPoints =
        calculatePlayerPoints(allPlayers, scoreModule);
    displayPlayerScores(player, playerPoints, match);
  }

  private boolean isValidForScoreDisplay(
      Match match, ScoreMatchModule scoreModule, StatsMatchModule statsModule) {
    return statsModule != null
        && scoreModule != null
        && match.getMap().getGamemodes().contains(Gamemode.SCOREBOX);
  }

  private List<MatchPlayer> getAllMatchPlayers(Match match) {
    List<MatchPlayer> allPlayers = new ArrayList<>(match.getParticipants());
    allPlayers.addAll(TowersForPGM.getInstance().getDisconnectedPlayers().values());
    return allPlayers;
  }

  private List<Map.Entry<MatchPlayer, Integer>> calculatePlayerPoints(
      List<MatchPlayer> players, ScoreMatchModule scoreModule) {
    List<Map.Entry<MatchPlayer, Integer>> playerPoints = new ArrayList<>();
    for (MatchPlayer mp : players) {
      int totalPoints = (int) scoreModule.getContribution(mp.getId());
      if (totalPoints > 0) playerPoints.add(Map.entry(mp, totalPoints));
    }
    playerPoints.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
    return playerPoints;
  }

  private void displayPlayerScores(
      MatchPlayer player, List<Map.Entry<MatchPlayer, Integer>> scores, Match match) {
    TowersForPGM.getInstance()
        .getServer()
        .getScheduler()
        .runTaskLater(
            TowersForPGM.getInstance(),
            () -> {
              int position = 1;
              int i = 0;
              while (i < scores.size()) {
                int currentPoints = scores.get(i).getValue();
                List<String> playersWithSamePoints = new ArrayList<>();

                while (i < scores.size() && scores.get(i).getValue() == currentPoints) {
                  MatchPlayer mp = scores.get(i).getKey();
                  boolean isConnected = match.getPlayers().contains(mp);
                  String playerName =
                      isConnected ? mp.getPrefixedName() : "§3" + mp.getNameLegacy();
                  playersWithSamePoints.add(playerName);
                  i++;
                }

                String allPlayers = String.join("§6, ", playersWithSamePoints);
                player.sendMessage(Component.text(
                    String.format("§7%d. %s §6- %d", position, allPlayers, currentPoints)));
                position++;
              }
            },
            1L);
  }

  private void displayDraftInfo(MatchPlayer player) {
    Match match = player.getMatch();
    DraftContext ctx = getDraftContext(match);
    if (ctx == null) return;

    DraftPhase phase = ctx.phase();
    if (phase != DraftPhase.RUNNING && phase != DraftPhase.ENDED) return;

    Captains captains = ctx.captains();
    Teams teams = ctx.teams();
    AvailablePlayers availablePlayers = ctx.availablePlayers();

    Player captain1Player = Bukkit.getPlayer(captains.getCaptain1());
    Player captain2Player = Bukkit.getPlayer(captains.getCaptain2());
    if (captain1Player == null || captain2Player == null) return;

    player.playSound(Sounds.INVENTORY_CLICK);
    player.sendMessage(Component.translatable("draft.captains.captainsHeader"));

    Component captainsLine = Component.text(teams.getTeamColor(1) + captain1Player.getName()
        + " §l§bvs. "
        + teams.getTeamColor(2) + captain2Player.getName());

    List<Map.Entry<String, Integer>> pickHistory = availablePlayers.getPickHistory();
    if (!pickHistory.isEmpty()) {
      StringBuilder hoverText = new StringBuilder();
      List<String> groupedPicks = groupPicksByIndex(pickHistory, teams);
      for (int i = 0; i < groupedPicks.size(); i++) {
        hoverText.append(i + 1).append(". ").append(groupedPicks.get(i));
        if (i < groupedPicks.size() - 1) hoverText.append("\n");
      }
      captainsLine =
          captainsLine.hoverEvent(HoverEvent.showText(Component.text(hoverText.toString())));
    }
    player.sendMessage(captainsLine);
  }

  private List<String> groupPicksByIndex(
      List<Map.Entry<String, Integer>> pickHistory, Teams teams) {
    List<String> groupedPicks = new ArrayList<>();
    int currentIndex = 0;
    StringBuilder currentGroup = new StringBuilder();

    for (int i = 0; i < pickHistory.size(); i++) {
      Map.Entry<String, Integer> pick = pickHistory.get(i);
      String teamColor = teams.getTeamColor(pick.getValue());

      if (i > 0 && i != currentIndex) {
        groupedPicks.add(currentGroup.toString());
        currentGroup = new StringBuilder();
        currentIndex = i;
      }

      if (currentGroup.length() > 0) currentGroup.append(", ");
      currentGroup.append(teamColor).append(pick.getKey());

      if (i == pickHistory.size() - 1) groupedPicks.add(currentGroup.toString());
    }

    return groupedPicks;
  }

  private boolean isDraftActive(Match match) {
    DraftContext ctx = getDraftContext(match);
    return ctx != null && ctx.captains().isMatchWithCaptains();
  }

  private DraftContext getDraftContext(Match match) {
    if (match == null) return null;
    var session = MatchSessionRegistry.get(match);
    if (session == null) return null;
    return session.getDraft();
  }

  private String removeForceFlag(String command) {
    return command.substring(0, command.length() - FORCE_FLAG.length());
  }
}
