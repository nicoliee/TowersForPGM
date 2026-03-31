package org.nicolie.towersforpgm.rankeds.queue;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.draft.map.MapVoteConfig.VoteMode;
import org.nicolie.towersforpgm.draft.map.MapVoteConfig.VoterMode;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.embeds.RankedStart;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.rankeds.RankedItem;
import org.nicolie.towersforpgm.rankeds.RankedPlayers;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.session.draft.DraftOptions;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;

public class MatchStarter {

  private final TowersForPGM plugin;
  private final QueueState queueState;
  private final QueueManager queueManager;
  private final RankedQueue rankedQueue;

  public MatchStarter(QueueManager queueManager) {
    this.plugin = TowersForPGM.getInstance();
    this.queueState = QueueState.getInstance();
    this.queueManager = queueManager;
    this.rankedQueue = RankedQueue.getInstance();
  }

  public void startMatch(Match match, String table) {
    int validSize = queueManager.getValidRankedSize(match);
    removeDisconnectedPlayersFromQueue(match, validSize);

    int currentSize = rankedQueue.size();
    if (currentSize % 2 != 0) currentSize--;
    int finalSize = Math.min(validSize, currentSize);

    if (currentSize < plugin.config().ranked().getRankedMinSize()) {
      queueState.setRanked(false);
      match.sendWarning(Queue.RANKED_PREFIX
          .append(Component.space())
          .append(Component.translatable("broadcast.startCancel")));
      return;
    }

    List<String> rankedPlayers = getRankedPlayerNames(finalSize);
    List<MatchPlayer> rankedMatchPlayers = getRankedMatchPlayers(rankedPlayers);

    RankedItem.removeItemToPlayers(rankedMatchPlayers);
    removePlayersFromQueue(finalSize);

    processEloAndStartMatch(match, table, rankedPlayers, rankedMatchPlayers);
  }

  public static void sendRankedStartEmbed(MatchStartEvent event) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    QueueState queueState = QueueState.getInstance();

    boolean ranked = queueState.isRanked();
    boolean rankedTable = plugin.config().databaseTables().currentTableIsRanked();

    if (!plugin.isMatchBotEnabled() || !rankedTable || !ranked) return;

    List<String> usernames = event.getMatch().getParticipants().stream()
        .map(MatchPlayer::getNameLegacy)
        .collect(Collectors.toList());

    String table = plugin.config().databaseTables().getRankedDefaultTable();
    var eloFuture =
        queueState.getEloCacheOrDefault(table, StatsManager.getEloForUsernames(table, usernames));

    eloFuture
        .thenAccept(eloChanges -> {
          EmbedBuilder embed = RankedStart.create(event.getMatch(), eloChanges);
          DiscordBot.sendMatchEmbed(
              embed,
              MatchBotConfig.getDiscordChannel(),
              null,
              DiscordBot.setEmbedThumbnail(event.getMatch().getMap(), embed));
          queueState.removeEloCache(table);
        })
        .exceptionally(throwable -> {
          plugin
              .getLogger()
              .severe("Error al obtener ELO para match start: " + throwable.getMessage());
          queueState.removeEloCache(table);
          return null;
        });
  }

  private void removeDisconnectedPlayersFromQueue(Match match, int validSize) {
    List<UUID> snapshot = rankedQueue.take(validSize);
    snapshot.stream()
        .filter(uuid -> PGM.get().getMatchManager().getPlayer(uuid) == null)
        .forEach(uuid -> {
          OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
          if (op.getName() != null) rankedQueue.remove(uuid);
        });
  }

  private List<String> getRankedPlayerNames(int finalSize) {
    return rankedQueue.take(finalSize).stream()
        .map(uuid -> {
          MatchPlayer p = PGM.get().getMatchManager().getPlayer(uuid);
          return p != null ? p.getNameLegacy() : null;
        })
        .filter(name -> name != null)
        .collect(Collectors.toList());
  }

  private List<MatchPlayer> getRankedMatchPlayers(List<String> rankedPlayers) {
    return rankedPlayers.stream()
        .map(username ->
            PGM.get().getMatchManager().getPlayer(queueManager.getUUIDFromUsername(username)))
        .filter(player -> player != null)
        .collect(Collectors.toList());
  }

  private void removePlayersFromQueue(int finalSize) {
    rankedQueue.take(finalSize).forEach(rankedQueue::remove);
  }

  private void processEloAndStartMatch(
      Match match, String table, List<String> rankedPlayers, List<MatchPlayer> rankedMatchPlayers) {
    var eloFuture = StatsManager.getEloForUsernames(table, rankedPlayers);
    queueState.putEloCache(table, eloFuture);

    eloFuture
        .thenAccept(eloList -> startDraftOrMatchmaking(match, eloList, rankedMatchPlayers))
        .exceptionally(throwable -> {
          plugin.getLogger().severe("Error al obtener ELO: " + throwable.getMessage());
          match.sendWarning(Queue.RANKED_PREFIX
              .append(Component.space())
              .append(Component.translatable("command.emptyResult")));
          queueState.removeEloCache(table);
          return null;
        });
  }

  private void startDraftOrMatchmaking(
      Match match, List<PlayerEloChange> eloList, List<MatchPlayer> rankedMatchPlayers) {
    List<Map.Entry<MatchPlayer, Integer>> playersWithElo = eloList.stream()
        .map(e -> new AbstractMap.SimpleEntry<>(
            PGM.get()
                .getMatchManager()
                .getPlayer(queueManager.getUUIDFromUsername(e.getUsername())),
            e.getCurrentElo()))
        .filter(entry -> entry.getKey() != null)
        .collect(Collectors.toList());

    RankedPlayers pair = RankedPlayers.selectCaptains(playersWithElo);

    if (plugin.config().ranked().isRankedMatchmaking()) {
      MatchSessionRegistry.of(match)
          .startMatchmaking(pair.getCaptain1(), pair.getCaptain2(), pair.getRemainingPlayers());
    } else {
      String mapVoteMode = plugin.config().ranked().getMapVote();
      DraftOptions.Builder builder = DraftOptions.builder()
          .orderPattern(plugin.config().ranked().getRankedOrder())
          .minOrder(0)
          .randomizeOrder(!pair.is2v2());

      switch (mapVoteMode == null ? "" : mapVoteMode.toLowerCase()) {
        case "veto":
          builder
              .mapVote(true)
              .maps(
                  TowersForPGM.getInstance().config().ranked().getRankedMaps(),
                  VoterMode.CAPTAINS_ONLY,
                  VoteMode.VETO,
                  15);
          break;
        case "plurality":
          builder
              .mapVote(true)
              .maps(
                  TowersForPGM.getInstance().config().ranked().getRankedMaps(),
                  VoterMode.ALL,
                  VoteMode.PLURALITY,
                  15);
          break;
        case "automatic":
        default:
          builder
              .mapVote(true)
              .maps(
                  TowersForPGM.getInstance().config().ranked().getRankedMaps(),
                  VoterMode.ALL,
                  VoteMode.AUTOMATIC,
                  0);
      }

      DraftOptions options = builder.build();
      MatchSessionRegistry.of(match)
          .startDraft(
              pair.getCaptain1(), pair.getCaptain2(), pair.getRemainingPlayers(), options, false);
    }
  }
}
