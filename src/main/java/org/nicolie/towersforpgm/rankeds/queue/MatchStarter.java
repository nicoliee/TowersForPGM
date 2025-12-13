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
import org.nicolie.towersforpgm.draft.core.Draft;
import org.nicolie.towersforpgm.draft.core.Matchmaking;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.embeds.RankedStart;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.rankeds.RankedItem;
import org.nicolie.towersforpgm.rankeds.RankedPlayers;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;

public class MatchStarter {
  private final TowersForPGM plugin;
  private final QueueState queueState;
  private final QueueManager queueManager;
  private final Draft draft;
  private final Matchmaking matchmaking;

  public MatchStarter(QueueManager queueManager, Draft draft, Matchmaking matchmaking) {
    this.plugin = TowersForPGM.getInstance();
    this.queueState = QueueState.getInstance();
    this.queueManager = queueManager;
    this.draft = draft;
    this.matchmaking = matchmaking;
  }

  public void startMatch(Match match, String table) {
    removeDisconnectedPlayersFromQueue(match);

    int validSize = queueManager.getValidRankedSize(match);
    int currentSize = queueState.getQueueSize();
    if (currentSize % 2 != 0) currentSize--;
    int finalSize = Math.min(validSize, currentSize);

    if (currentSize < plugin.config().ranked().getRankedMinSize()) {
      queueState.setRanked(false);
      match.sendWarning(
          Component.text(getRankedPrefix() + LanguageManager.message("ranked.cancelled")));
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

    List<String> usernames = event.getMatch().getPlayers().stream()
        .map(MatchPlayer::getNameLegacy)
        .collect(Collectors.toList());

    String table = plugin.config().databaseTables().getRankedDefaultTable();
    java.util.concurrent.CompletableFuture<List<PlayerEloChange>> eloFuture =
        queueState.getEloCacheOrDefault(table, StatsManager.getEloForUsernames(table, usernames));

    eloFuture
        .thenAccept(eloChanges -> {
          EmbedBuilder embed = RankedStart.create(event.getMatch(), eloChanges);
          DiscordBot.setEmbedThumbnail(event.getMatch().getMap(), embed);
          DiscordBot.sendMatchEmbed(
              embed, event.getMatch(), MatchBotConfig.getDiscordChannel(), null);
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

  private void removeDisconnectedPlayersFromQueue(Match match) {
    List<UUID> disconnectedUUIDs = queueState.getQueuePlayers().stream()
        .filter(uuid -> PGM.get().getMatchManager().getPlayer(uuid) == null)
        .collect(Collectors.toList());

    for (UUID disconnectedUUID : disconnectedUUIDs) {
      OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(disconnectedUUID);
      if (offlinePlayer.getName() != null) {
        queueState.removePlayer(disconnectedUUID);
      }
    }
  }

  private List<String> getRankedPlayerNames(int finalSize) {
    return queueState.getQueuePlayers().subList(0, finalSize).stream()
        .map(uuid -> {
          MatchPlayer player = PGM.get().getMatchManager().getPlayer(uuid);
          return player != null ? player.getNameLegacy() : null;
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
    List<UUID> queuePlayers = queueState.getQueuePlayers();
    for (int i = 0; i < finalSize && i < queuePlayers.size(); i++) {
      queueState.removePlayer(queuePlayers.get(i));
    }
  }

  private void processEloAndStartMatch(
      Match match, String table, List<String> rankedPlayers, List<MatchPlayer> rankedMatchPlayers) {
    java.util.concurrent.CompletableFuture<List<PlayerEloChange>> eloFuture =
        StatsManager.getEloForUsernames(table, rankedPlayers);
    queueState.putEloCache(table, eloFuture);

    eloFuture
        .thenAccept(eloList -> startDraftOrMatchmaking(match, eloList, rankedMatchPlayers))
        .exceptionally(throwable -> {
          plugin
              .getLogger()
              .severe(LanguageManager.message("ranked.error.getElo") + throwable.getMessage());
          match.sendWarning(
              Component.text(getRankedPrefix() + LanguageManager.message("ranked.error.getData")));
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
      matchmaking.startMatchmaking(
          pair.getCaptain1(), pair.getCaptain2(), pair.getRemainingPlayers(), match);
    } else {
      boolean randomizeOrder = !pair.is2v2();
      draft.setCustomOrderPattern(plugin.config().ranked().getRankedOrder(), 0);
      draft.startDraft(
          pair.getCaptain1(),
          pair.getCaptain2(),
          pair.getRemainingPlayers(),
          match,
          randomizeOrder);
    }
  }

  private String getRankedPrefix() {
    return LanguageManager.message("ranked.prefix");
  }
}
