package org.nicolie.towersforpgm.rankeds;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitTask;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.Matchmaking;
import org.nicolie.towersforpgm.draft.Teams;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.embeds.RankedStart;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public class Queue {
  private static Boolean ranked = false;
  private static boolean countdownActive = false;
  private static final List<UUID> queuePlayers = new ArrayList<>();
  private static final java.util.Map<
          String, java.util.concurrent.CompletableFuture<java.util.List<PlayerEloChange>>>
      eloCache = new java.util.concurrent.ConcurrentHashMap<>();

  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final Draft draft;
  private final Matchmaking matchmaking;
  private final Teams teams;

  public Queue(Draft draft, Matchmaking matchmaking, Teams teams) {
    this.draft = draft;
    this.matchmaking = matchmaking;
    this.teams = teams;
  }

  private String getRankedPrefix() {
    return LanguageManager.langMessage("ranked.prefix");
  }

  private int getValidRankedSize(Match match) {
    int minSize = ConfigManager.getRankedMinSize();
    int maxSize = ConfigManager.getRankedMaxSize();
    int onlinePlayers = match.getPlayers().size();

    int target = Math.min(maxSize, onlinePlayers);

    if (target % 2 != 0) {
      target--;
    }

    return target >= minSize ? target : minSize;
  }

  private int getCountdownTime(Match match) {
    int minSize = ConfigManager.getRankedMinSize();
    return (match.getPlayers().size() == minSize || match.getPlayers().size() == minSize + 1)
        ? 5
        : 15;
  }

  public void addPlayer(MatchPlayer player) {
    UUID playerUUID = player.getId();
    Match match = player.getMatch();
    String map = match.getMap().getName();

    if (player.isParticipating()
        || match.isRunning()
        || teams.isPlayerInAnyTeam(player.getNameLegacy())) {
      player.sendWarning(Component.text(
          getRankedPrefix() + LanguageManager.langMessage("ranked.matchInProgress")));
      return;
    }

    if (queuePlayers.contains(playerUUID)) {
      player.sendWarning(
          Component.text(getRankedPrefix() + LanguageManager.langMessage("ranked.alreadyInQueue")));
      return;
    }

    if (!ConfigManager.getRankedMaps().contains(map)) {
      player.sendWarning(Component.text(getRankedPrefix()
          + LanguageManager.langMessage("ranked.notRankedMap").replace("{map}", map)));
      return;
    }

    queuePlayers.add(playerUUID);

    int targetSize = getValidRankedSize(match);
    Component message = Component.text(getRankedPrefix()
        + LanguageManager.langMessage("ranked.joinedQueue")
            .replace("{player}", player.getPrefixedName())
            .replace("{size}", String.valueOf(queuePlayers.size()))
            .replace(
                "{target}",
                String.valueOf(targetSize > 0 ? targetSize : ConfigManager.getRankedMinSize())));

    match.sendMessage(message);

    if (queuePlayers.size() >= ConfigManager.getRankedMinSize()) {
      startRanked(match);
    }
  }

  public void removePlayer(MatchPlayer player) {
    UUID playerUUID = player.getId();

    if (!queuePlayers.contains(playerUUID)) {
      player.sendWarning(
          Component.text(getRankedPrefix() + LanguageManager.langMessage("ranked.notInQueue")));
      return;
    }

    queuePlayers.remove(playerUUID);
    Match match = player.getMatch();
    int targetSize = getValidRankedSize(match);

    Component message = Component.text(getRankedPrefix()
        + LanguageManager.langMessage("ranked.leftQueue")
            .replace("{player}", player.getPrefixedName())
            .replace("{size}", String.valueOf(queuePlayers.size()))
            .replace(
                "{target}",
                String.valueOf(targetSize > 0 ? targetSize : ConfigManager.getRankedMinSize())));

    match.sendMessage(message);
  }

  public void startRanked(Match match) {
    if (match.getPhase() == MatchPhase.RUNNING
        || match.getPhase() == MatchPhase.FINISHED
        || countdownActive
        || ranked) {
      return;
    }

    countdownActive = true;
    final AtomicInteger countdown = new AtomicInteger(getCountdownTime(match));
    final int maxSize = ConfigManager.getRankedMaxSize();
    final int minSize = ConfigManager.getRankedMinSize();

    final BukkitTask[] taskRef = new BukkitTask[1];
    taskRef[0] = Bukkit.getScheduler()
        .runTaskTimer(
            plugin,
            () -> {
              int timeLeft = countdown.get();

              if (timeLeft <= 0) {
                String table = ConfigManager.getRankedDefaultTable();
                ConfigManager.addTemp(table);
                queue(match, table);
                taskRef[0].cancel();
                countdownActive = false;
                ranked = true;
                return;
              }

              if (queuePlayers.size() < minSize) {
                taskRef[0].cancel();
                countdownActive = false;
                match.sendWarning(Component.text(
                    getRankedPrefix() + LanguageManager.langMessage("ranked.cancelled")));
                return;
              }

              if (queuePlayers.size() >= maxSize) {
                countdown.set(Math.min(timeLeft, 5));
                timeLeft = countdown.get();
              }

              match.sendMessage(Component.text(getRankedPrefix()
                  + LanguageManager.langMessage("ranked.countdown")
                      .replace("{time}", String.valueOf(timeLeft))));
              match.playSound(Sounds.INVENTORY_CLICK);

              countdown.decrementAndGet();
            },
            0L,
            20L);
  }

  private void queue(Match match, String table) {
    int validSize = getValidRankedSize(match);
    int currentSize = queuePlayers.size();
    if (currentSize % 2 != 0) {
      currentSize--;
    }
    int finalSize = Math.min(validSize, currentSize);
    if (currentSize < ConfigManager.getRankedMinSize()) {
      ranked = false;
      return;
    }
    List<String> rankedPlayers = queuePlayers.subList(0, finalSize).stream()
        .map(uuid -> PGM.get().getMatchManager().getPlayer(uuid).getNameLegacy())
        .collect(Collectors.toList());

    List<MatchPlayer> rankedMatchPlayers = rankedPlayers.stream()
        .map(username -> PGM.get().getMatchManager().getPlayer(getUUIDFromUsername(username)))
        .collect(Collectors.toList());

    ItemListener.removeItemToPlayers(rankedMatchPlayers);
    queuePlayers.subList(0, finalSize).clear();

    java.util.concurrent.CompletableFuture<java.util.List<PlayerEloChange>> eloFuture =
        StatsManager.getEloForUsernames(table, rankedPlayers);

    eloCache.put(table, eloFuture);

    eloFuture
        .thenAccept(eloList -> {
          List<Map.Entry<MatchPlayer, Integer>> playersWithElo = eloList.stream()
              .map(e -> {
                UUID uuid = getUUIDFromUsername(e.getUsername());
                MatchPlayer player = PGM.get().getMatchManager().getPlayer(uuid);
                return new AbstractMap.SimpleEntry<>(player, e.getCurrentElo());
              })
              .filter(entry -> entry.getKey() != null)
              .collect(Collectors.toList());

          RankedPlayers pair = RankedPlayers.selectCaptains(playersWithElo);

          if (ConfigManager.isRankedMatchmaking()) {
            matchmaking.startMatchmaking(
                pair.getCaptain1(), pair.getCaptain2(), pair.getRemainingPlayers(), match);
          } else {
            draft.setCustomOrderPattern(ConfigManager.getRankedOrder(), 0);
            draft.startDraft(
                pair.getCaptain1(), pair.getCaptain2(), pair.getRemainingPlayers(), match, true);
          }
        })
        .exceptionally(throwable -> {
          plugin
              .getLogger()
              .severe(LanguageManager.langMessage("ranked.error.getElo") + throwable.getMessage());
          match.sendWarning(Component.text(
              getRankedPrefix() + LanguageManager.langMessage("ranked.error.getData")));
          eloCache.remove(table);
          return null;
        });
  }

  public static void sendRankedStartEmbed(MatchStartEvent event) {
    TowersForPGM plugin = TowersForPGM.getInstance();
    boolean rankedTable = ConfigManager.isRankedTable(
        ConfigManager.getActiveTable(event.getMatch().getMap().getName()));
    boolean ranked = Queue.isRanked();
    boolean matchbot = plugin.isMatchBotEnabled();

    if (matchbot && (rankedTable && ranked)) {
      Collection<MatchPlayer> players = event.getMatch().getPlayers();
      List<String> usernames = new ArrayList<>();
      for (MatchPlayer player : players) {
        usernames.add(player.getNameLegacy());
      }
      String table = ConfigManager.getRankedDefaultTable();

      java.util.concurrent.CompletableFuture<java.util.List<PlayerEloChange>> eloFuture =
          eloCache.getOrDefault(table, StatsManager.getEloForUsernames(table, usernames));

      eloFuture
          .thenAccept(eloChanges -> {
            EmbedBuilder embed = RankedStart.create(event.getMatch(), eloChanges);
            DiscordBot.setEmbedThumbnail(event.getMatch().getMap(), embed);
            DiscordBot.sendMatchEmbed(
                embed, event.getMatch(), MatchBotConfig.getDiscordChannel(), null);
            eloCache.remove(table);
          })
          .exceptionally(throwable -> {
            plugin
                .getLogger()
                .severe("Error al obtener ELO para match start: " + throwable.getMessage());
            eloCache.remove(table);
            return null;
          });
    }
  }

  public List<String> getQueueList() {
    List<String> players = new ArrayList<>();
    for (UUID uuid : queuePlayers) {
      MatchPlayer player = PGM.get().getMatchManager().getPlayer(uuid);
      if (player != null) {
        players.add(player.getPrefixedName());
      }
    }
    return players;
  }

  public static int getQueueSize() {
    return queuePlayers.size();
  }

  public List<UUID> getQueuePlayers() {
    return new ArrayList<>(queuePlayers);
  }

  public int getTargetSize(Match match) {
    return getValidRankedSize(match);
  }

  public static void clearQueue() {
    queuePlayers.clear();
    countdownActive = false;
  }

  private UUID getUUIDFromUsername(String username) {
    OfflinePlayer offline = Bukkit.getPlayerExact(username);
    return offline != null ? offline.getUniqueId() : null;
  }

  public static Boolean isRanked() {
    return ranked;
  }

  public static void setRanked(Boolean value) {
    ranked = value;
  }
}
