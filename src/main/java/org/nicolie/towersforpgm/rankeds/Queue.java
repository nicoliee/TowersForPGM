package org.nicolie.towersforpgm.rankeds;

import java.util.AbstractMap;
import java.util.ArrayList;
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
import org.nicolie.towersforpgm.matchbot.rankeds.listeners.RankedListener;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.MatchManager;
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
  private static final Map<String, java.util.concurrent.CompletableFuture<List<PlayerEloChange>>>
      eloCache = new java.util.concurrent.ConcurrentHashMap<>();
  private static Queue instance;

  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final Draft draft;
  private final Matchmaking matchmaking;
  private final Teams teams;

  public Queue(Draft draft, Matchmaking matchmaking, Teams teams) {
    this.draft = draft;
    this.matchmaking = matchmaking;
    this.teams = teams;
    instance = this;
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
    sendQueueMessage(match, player.getPrefixedName(), false);
  }

  public void addPlayer(UUID playerUUID, Match match) {
    if (playerUUID == null) return;
    if (match == null) match = MatchManager.getMatch();

    MatchPlayer onlinePlayer = PGM.get().getMatchManager().getPlayer(playerUUID);
    String playerName;

    if (onlinePlayer != null) {
      playerName = onlinePlayer.getPrefixedName();
    } else {
      OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
      if (offlinePlayer.getName() == null) return;
      playerName = "§3" + offlinePlayer.getName();
    }

    if (queuePlayers.contains(playerUUID)) return;

    String map = match.getMap().getName();
    if (!ConfigManager.getRankedMaps().contains(map)) {
      return;
    }

    queuePlayers.add(playerUUID);
    sendQueueMessage(match, playerName, false);
  }

  public void removePlayer(MatchPlayer player) {
    UUID playerUUID = player.getId();
    if (!queuePlayers.contains(playerUUID)) {
      player.sendWarning(
          Component.text(getRankedPrefix() + LanguageManager.langMessage("ranked.notInQueue")));
      return;
    }
    queuePlayers.remove(playerUUID);
    if (ranked && !countdownActive) ranked = false;
    sendQueueMessage(player.getMatch(), player.getPrefixedName(), true);
  }

  public void removePlayer(UUID playerUUID, Match match) {
    if (playerUUID == null) return;
    if (match == null) match = MatchManager.getMatch();

    MatchPlayer onlinePlayer = PGM.get().getMatchManager().getPlayer(playerUUID);
    String playerName;

    if (onlinePlayer != null) {
      playerName = onlinePlayer.getPrefixedName();
    } else {
      OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
      if (offlinePlayer.getName() == null) return;
      playerName = "§3" + offlinePlayer.getName();
    }

    if (!queuePlayers.contains(playerUUID)) return;

    queuePlayers.remove(playerUUID);
    sendQueueMessage(match, playerName, true);
  }

  public void startRanked(Match match) {
    if (match.getPhase() == MatchPhase.RUNNING
        || match.getPhase() == MatchPhase.FINISHED
        || countdownActive
        || ranked) return;

    ranked = false;
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
              List<UUID> disconnectedPlayerUUIDs = getDisconnectedPlayersInQueue();

              if (timeLeft <= 0) {
                removeDisconnectedPlayers(match, disconnectedPlayerUUIDs);
                if (queuePlayers.size() < minSize) {
                  cancelRanked(match, taskRef[0]);
                  return;
                }
                String table = ConfigManager.getRankedDefaultTable();
                ConfigManager.addTemp(table);
                queue(match, table);
                taskRef[0].cancel();
                countdownActive = false;
                ranked = true;
                return;
              }

              if (queuePlayers.size() < minSize || match.isRunning()) {
                cancelRanked(match, taskRef[0]);
                return;
              }

              if (!disconnectedPlayerUUIDs.isEmpty()) {
                List<String> disconnectedNames = disconnectedPlayerUUIDs.stream()
                    .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                    .filter(name -> name != null)
                    .collect(Collectors.toList());

                if (timeLeft > 30) {
                  countdown.set(30);
                  timeLeft = 30;
                }
                match.sendActionBar(Component.text(getRankedPrefix()
                    + LanguageManager.langMessage("ranked.waitingFor")
                        .replace("{player}", String.join(", ", disconnectedNames))));
              } else {
                if (queuePlayers.size() >= maxSize && timeLeft > 5) {
                  countdown.set(5);
                  timeLeft = 5;
                } else if (queuePlayers.size() < maxSize && timeLeft > 15 && timeLeft <= 30) {
                  countdown.set(15);
                  timeLeft = 15;
                }
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
    List<UUID> disconnectedUUIDs = queuePlayers.stream()
        .filter(uuid -> PGM.get().getMatchManager().getPlayer(uuid) == null)
        .collect(Collectors.toList());

    for (UUID disconnectedUUID : disconnectedUUIDs) {
      OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(disconnectedUUID);
      if (offlinePlayer.getName() != null) {
        queuePlayers.remove(disconnectedUUID);
        sendQueueMessage(match, offlinePlayer.getName(), true);
      }
    }

    int validSize = getValidRankedSize(match);
    int currentSize = queuePlayers.size();
    if (currentSize % 2 != 0) currentSize--;
    int finalSize = Math.min(validSize, currentSize);

    if (currentSize < ConfigManager.getRankedMinSize()) {
      ranked = false;
      match.sendWarning(
          Component.text(getRankedPrefix() + LanguageManager.langMessage("ranked.cancelled")));
      return;
    }

    List<String> rankedPlayers = queuePlayers.subList(0, finalSize).stream()
        .map(uuid -> {
          MatchPlayer player = PGM.get().getMatchManager().getPlayer(uuid);
          return player != null ? player.getNameLegacy() : null;
        })
        .filter(name -> name != null)
        .collect(Collectors.toList());

    List<MatchPlayer> rankedMatchPlayers = rankedPlayers.stream()
        .map(username -> PGM.get().getMatchManager().getPlayer(getUUIDFromUsername(username)))
        .filter(player -> player != null)
        .collect(Collectors.toList());

    ItemListener.removeItemToPlayers(rankedMatchPlayers);
    queuePlayers.subList(0, finalSize).clear();

    java.util.concurrent.CompletableFuture<List<PlayerEloChange>> eloFuture =
        StatsManager.getEloForUsernames(table, rankedPlayers);
    eloCache.put(table, eloFuture);

    eloFuture
        .thenAccept(eloList -> {
          List<Map.Entry<MatchPlayer, Integer>> playersWithElo = eloList.stream()
              .map(e -> new AbstractMap.SimpleEntry<>(
                  PGM.get().getMatchManager().getPlayer(getUUIDFromUsername(e.getUsername())),
                  e.getCurrentElo()))
              .filter(entry -> entry.getKey() != null)
              .collect(Collectors.toList());

          RankedPlayers pair = RankedPlayers.selectCaptains(playersWithElo);

          if (ConfigManager.isRankedMatchmaking()) {
            matchmaking.startMatchmaking(
                pair.getCaptain1(), pair.getCaptain2(), pair.getRemainingPlayers(), match);
          } else {
            boolean randomizeOrder = !pair.is2v2();
            draft.setCustomOrderPattern(ConfigManager.getRankedOrder(), 0);
            draft.startDraft(
                pair.getCaptain1(),
                pair.getCaptain2(),
                pair.getRemainingPlayers(),
                match,
                randomizeOrder);
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

    if (!plugin.isMatchBotEnabled() || !rankedTable || !ranked) return;

    List<String> usernames = event.getMatch().getPlayers().stream()
        .map(MatchPlayer::getNameLegacy)
        .collect(Collectors.toList());

    String table = ConfigManager.getRankedDefaultTable();
    java.util.concurrent.CompletableFuture<List<PlayerEloChange>> eloFuture =
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

  public List<String> getQueueList() {
    return queuePlayers.stream()
        .map(uuid -> {
          MatchPlayer player = PGM.get().getMatchManager().getPlayer(uuid);
          if (player != null) {
            return player.getPrefixedName();
          } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            return offlinePlayer.getName() != null ? "§3" + offlinePlayer.getName() : null;
          }
        })
        .filter(name -> name != null)
        .collect(Collectors.toList());
  }

  public List<UUID> getQueuePlayers() {
    return new ArrayList<>(queuePlayers);
  }

  public int getTargetSize(Match match) {
    return getValidRankedSize(match);
  }

  public static int getQueueSize() {
    return queuePlayers.size();
  }

  public static void clearQueue() {
    queuePlayers.clear();
    countdownActive = false;
  }

  public static Boolean isRanked() {
    return ranked;
  }

  public static void setRanked(Boolean value) {
    ranked = value;
  }

  public static Queue getQueue() {
    return instance;
  }

  private void sendQueueMessage(Match match, String playerName, boolean isLeave) {
    int targetSize = getValidRankedSize(match);
    String messageKey = isLeave ? "ranked.leftQueue" : "ranked.joinedQueue";

    Component message = Component.text(getRankedPrefix()
        + LanguageManager.langMessage(messageKey)
            .replace("{player}", playerName)
            .replace("{size}", String.valueOf(queuePlayers.size()))
            .replace(
                "{target}",
                String.valueOf(targetSize > 0 ? targetSize : ConfigManager.getRankedMinSize())));

    match.sendMessage(message);

    if (!isLeave && queuePlayers.size() >= ConfigManager.getRankedMinSize()) {
      if (ranked && !countdownActive) ranked = false;
      startRanked(match);
    }
  }

  private void cancelRanked(Match match, BukkitTask task) {
    task.cancel();
    countdownActive = false;
    ranked = false;
    match.sendWarning(
        Component.text(getRankedPrefix() + LanguageManager.langMessage("ranked.cancelled")));
  }

  private void removeDisconnectedPlayers(Match match, List<UUID> disconnectedPlayerUUIDs) {
    for (UUID playerUUID : disconnectedPlayerUUIDs) {
      if (playerUUID != null && queuePlayers.contains(playerUUID)) {
        String playerName = Bukkit.getOfflinePlayer(playerUUID).getName();
        queuePlayers.remove(playerUUID);
        sendQueueMessage(match, "§3" + (playerName != null ? playerName : "Unknown"), true);
        RankedListener.movePlayerToInactive(playerUUID);
      }
    }
  }

  private List<UUID> getDisconnectedPlayersInQueue() {
    return queuePlayers.stream()
        .filter(uuid -> PGM.get().getMatchManager().getPlayer(uuid) == null)
        .collect(Collectors.toList());
  }

  private UUID getUUIDFromUsername(String username) {
    OfflinePlayer offline = Bukkit.getPlayerExact(username);
    return offline != null ? offline.getUniqueId() : null;
  }

  private String getRankedPrefix() {
    return LanguageManager.langMessage("ranked.prefix");
  }

  private int getValidRankedSize(Match match) {
    int minSize = ConfigManager.getRankedMinSize();
    int maxSize = ConfigManager.getRankedMaxSize();
    int target = Math.min(maxSize, match.getPlayers().size());
    if (target % 2 != 0) target--;
    return target >= minSize ? target : minSize;
  }

  private int getCountdownTime(Match match) {
    int minSize = ConfigManager.getRankedMinSize();
    int maxSize = ConfigManager.getRankedMaxSize();
    if (!getDisconnectedPlayersInQueue().isEmpty()) return 30;
    if (queuePlayers.size() >= maxSize) return 5;
    int playerCount = match.getPlayers().size();
    return (playerCount == minSize || playerCount == minSize + 1) ? 5 : 15;
  }
}
