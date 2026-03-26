package org.nicolie.towersforpgm.rankeds.disconnect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.tables.TableInfo;
import org.nicolie.towersforpgm.database.MatchHistoryManager;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.database.models.history.MatchStats;
import org.nicolie.towersforpgm.database.models.history.TeamInfo;
import org.nicolie.towersforpgm.draft.team.Teams;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.embeds.Forfeit;
import org.nicolie.towersforpgm.matchbot.embeds.MatchInfo;
import org.nicolie.towersforpgm.rankeds.Elo;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.rankeds.Queue;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.stats.StatsMatchModule;

public final class SanctionHandler {

  private SanctionHandler() {}

  public static void apply(Match match, Party losingTeam, Teams teams) {
    String sanctionedUser = DisconnectManager.getSanctionedUsername(match);

    if (sanctionedUser == null) {
      TowersForPGM.getInstance().setStatsCancel(true);
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "end");
      return;
    }

    List<MatchPlayer> sanctionedTeam = new ArrayList<>(losingTeam.getPlayers());
    List<MatchPlayer> opponentTeam = buildOpponentTeam(match, losingTeam);

    List<MatchPlayer> onlinePlayers = new ArrayList<>(match.getParticipants());

    StatsMatchModule statsModule = match.getModule(StatsMatchModule.class);
    ScoreMatchModule scoreModule = match.getModule(ScoreMatchModule.class);

    // El sancionado es offline — se agrega manualmente con stats en 0 como perdedor
    List<Stats> rawStats = buildRawStats(match, onlinePlayers, sanctionedUser, losingTeam);
    Map<String, MatchStats> playerMatchStats = buildPlayerMatchStats(
        match, onlinePlayers, statsModule, scoreModule, sanctionedUser, losingTeam);
    List<TeamInfo> teamInfos = MatchHistoryManager.extractTeamInfo(match);
    Map<String, TeamInfo> playerTeamMap = MatchHistoryManager.createPlayerTeamMap(match);
    MatchInfo matchInfo = MatchInfo.getMatchInfo(match);
    String table = TowersForPGM.getInstance()
        .config()
        .databaseTables()
        .getTable(match.getMap().getName());

    Elo.doubleLossPenalty(sanctionedUser, sanctionedTeam, opponentTeam)
        .thenAccept(penalty -> Bukkit.getScheduler().runTask(TowersForPGM.getInstance(), () -> {
          TowersForPGM.getInstance().setStatsCancel(true);

          int realDelta = penalty.getNewElo() - penalty.getCurrentElo();

          StatsManager.applySanction(table, sanctionedUser, 2, penalty);

          match.sendMessage(Queue.RANKED_PREFIX
              .append(Component.space())
              .append(Component.translatable("ranked.forfeitSanctionApplied")
                  .color(NamedTextColor.RED)
                  .arguments(
                      Component.text(sanctionedUser).color(NamedTextColor.GRAY),
                      Component.text(realDelta).color(NamedTextColor.GRAY))));

          List<PlayerEloChange> eloChanges =
              buildEloChangesForHistory(onlinePlayers, sanctionedUser, penalty);

          saveMatchHistory(
              table, matchInfo, rawStats, eloChanges, teamInfos, playerTeamMap, playerMatchStats);

          sendEmbed(match, table, sanctionedUser, losingTeam, realDelta);

          Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "end");
        }));
  }

  /** Online players con sus stats reales + sancionado offline con todo en 0 como perdedor. */
  private static List<Stats> buildRawStats(
      Match match, List<MatchPlayer> onlinePlayers, String sanctionedUser, Party losingTeam) {

    List<Stats> result = new ArrayList<>();

    for (MatchPlayer player : onlinePlayers) {
      boolean isWinner = match.getWinners().contains(player.getCompetitor());
      result.add(new Stats(
          player.getNameLegacy(),
          0,
          0,
          0,
          0,
          0,
          0,
          0,
          0,
          isWinner ? 1 : 0,
          1,
          isWinner ? 1 : 0,
          0,
          0,
          0,
          0));
    }

    // Sancionado: perdedor, 1 partida, stats en 0
    result.add(new Stats(
        sanctionedUser,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0, // perdedor
        1,
        0,
        0,
        0,
        0,
        0));

    return result;
  }

  private static Map<String, MatchStats> buildPlayerMatchStats(
      Match match,
      List<MatchPlayer> onlinePlayers,
      StatsMatchModule statsModule,
      ScoreMatchModule scoreModule,
      String sanctionedUser,
      Party losingTeam) {

    Map<String, MatchStats> map = new HashMap<>();

    for (MatchPlayer player : onlinePlayers) {
      PlayerStats ps = statsModule != null ? statsModule.getPlayerStat(player) : null;
      String teamName =
          player.getCompetitor() != null ? player.getCompetitor().getDefaultName() : null;
      int totalPoints =
          (scoreModule != null && match.getMap().getGamemodes().contains(Gamemode.SCOREBOX))
              ? (int) scoreModule.getContribution(player.getId())
              : 0;
      map.put(
          player.getNameLegacy(),
          MatchHistoryManager.createMatchStats(ps, player.getNameLegacy(), totalPoints, teamName));
    }

    // Sancionado offline: MatchStats vacío con el nombre de su equipo
    String sanctionedTeamName = losingTeam.getDefaultName();
    map.put(
        sanctionedUser,
        MatchHistoryManager.createMatchStats(null, sanctionedUser, 0, sanctionedTeamName));

    return map;
  }

  /** Online players con delta 0. Sancionado con su penalty real (delta clampeado a -100). */
  private static List<PlayerEloChange> buildEloChangesForHistory(
      List<MatchPlayer> onlinePlayers, String sanctionedUser, PlayerEloChange penalty) {

    List<PlayerEloChange> changes = new ArrayList<>();

    for (MatchPlayer mp : onlinePlayers) {
      changes.add(new PlayerEloChange(mp.getNameLegacy(), 0, 0, 0, 0));
    }

    changes.add(penalty);

    return changes;
  }

  private static void saveMatchHistory(
      String table,
      MatchInfo matchInfo,
      List<Stats> rawStats,
      List<PlayerEloChange> eloChanges,
      List<TeamInfo> teamInfos,
      Map<String, TeamInfo> playerTeamMap,
      Map<String, MatchStats> playerMatchStats) {

    MatchHistoryManager.generateMatchId(table)
        .thenCompose(matchId -> {
          TowersForPGM.getInstance().config().database().setStatsLink(matchId);
          return MatchHistoryManager.saveMatch(
              matchId,
              table,
              matchInfo,
              true,
              rawStats,
              eloChanges,
              teamInfos,
              playerTeamMap,
              playerMatchStats);
        })
        .exceptionally(ex -> {
          TowersForPGM.getInstance()
              .getLogger()
              .warning("Error guardando historial sanction: " + ex.getMessage());
          return null;
        });
  }

  private static void sendEmbed(
      Match match, String table, String sanctionedUser, Party losingTeam, int realDelta) {
    if (!TowersForPGM.getInstance().isMatchBotEnabled()) return;

    TableInfo tableInfo = TowersForPGM.getInstance().config().databaseTables().getTableInfo(table);
    if (tableInfo == null || !tableInfo.isRanked()) return;

    Set<String> usernames = new HashSet<>();
    match.getPlayers().forEach(mp -> usernames.add(mp.getNameLegacy()));
    usernames.add(sanctionedUser);

    StatsManager.getEloForUsernames(table, new ArrayList<>(usernames)).thenAccept(eloList -> {
      // Sobrescribir el elo del sancionado con el valor actualizado
      eloList.removeIf(e -> e.getUsername().equals(sanctionedUser));
      eloList.add(new PlayerEloChange(
          sanctionedUser, penaltyNewElo(realDelta, sanctionedUser, eloList), 0, 0, 0));
      EmbedBuilder embed = Forfeit.create(
          match, table, eloList, sanctionedUser, losingTeam.getNameLegacy(), realDelta);
      DiscordBot.sendMatchEmbed(
          embed,
          MatchBotConfig.getDiscordChannel(),
          null,
          DiscordBot.setEmbedThumbnail(match.getMap(), embed));
    });
  }

  private static int penaltyNewElo(
      int realDelta, String sanctionedUser, List<PlayerEloChange> eloList) {
    // Buscar el elo anterior del sancionado (si existe)
    for (PlayerEloChange e : eloList) {
      if (e.getUsername().equals(sanctionedUser)) {
        return e.getCurrentElo() + realDelta;
      }
    }
    // Si no se encuentra, solo devolver el delta (fallback)
    return realDelta;
  }

  private static List<MatchPlayer> buildOpponentTeam(Match match, Party losingTeam) {
    List<MatchPlayer> opponent = new ArrayList<>();
    for (Party p : match.getParties()) {
      if (!p.equals(losingTeam)) opponent.addAll(p.getPlayers());
    }
    return opponent;
  }
}
