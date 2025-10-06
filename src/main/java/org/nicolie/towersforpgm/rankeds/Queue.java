package org.nicolie.towersforpgm.rankeds;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.nicolie.towersforpgm.MatchManager;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.draft.Matchmaking;
import org.nicolie.towersforpgm.draft.Teams;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public class Queue {
  private static Boolean ranked = false;
  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final Draft draft;
  private final Matchmaking matchmaking;
  private static final List<UUID> queuePlayers = new java.util.ArrayList<>();
  private static boolean countdownActive = false;
  private final Teams teams;

  private String getRankedPrefix() {
    return LanguageManager.langMessage("ranked.prefix");
  }

  public Queue(Draft draft, Matchmaking matchmaking, Teams teams) {
    this.draft = draft;
    this.matchmaking = matchmaking;
    this.teams = teams;
  }

  public void setSize(CommandSender sender, int size) {
    if (size == ConfigManager.getRankedSize()) {
      return;
    }
    if (size < 4 || size % 2 != 0) {
      MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer((Player) sender);
      matchPlayer.sendWarning(
          Component.text(getRankedPrefix() + LanguageManager.langMessage("ranked.sizeInvalid")));
      return;
    }
    Match match = PGM.get().getMatchManager().getMatch(sender);
    ConfigManager.setRankedSize(size);
    RankedPlayers.clearCaptainHistory();
    String message = getRankedPrefix()
        + LanguageManager.langMessage("ranked.sizeSet").replace("{size}", String.valueOf(size));
    match.sendMessage(Component.text(message));
    if (queuePlayers.size() >= ConfigManager.getRankedSize()) {
      startRanked(match);
    }
  }

  public boolean setSize(int size) {
    if (size == ConfigManager.getRankedSize()) {
      return false;
    }
    if (size < 4 || size % 2 != 0) {
      return false;
    }
    Match match = MatchManager.getMatch();
    ConfigManager.setRankedSize(size);
    RankedPlayers.clearCaptainHistory();
    String message = getRankedPrefix()
        + LanguageManager.langMessage("ranked.sizeSet").replace("{size}", String.valueOf(size));
    match.sendMessage(Component.text(message));
    if (queuePlayers.size() >= ConfigManager.getRankedSize()) {
      startRanked(match);
    }
    return true;
  }

  public void addPlayer(MatchPlayer player) {
    UUID playerUUID = player.getId();
    if (player.isParticipating()
        || player.getMatch().isRunning()
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
    Match match = player.getMatch();
    String map = match.getMap().getName();
    if (!ConfigManager.getRankedMaps().contains(map)) {
      player.sendWarning(Component.text(getRankedPrefix()
          + LanguageManager.langMessage("ranked.notRankedMap").replace("{map}", map)));
      return;
    }
    queuePlayers.add(playerUUID);
    Component message = Component.text(getRankedPrefix()
        + LanguageManager.langMessage("ranked.joinedQueue")
            .replace("{player}", player.getPrefixedName())
            .replace("{size}", String.valueOf(queuePlayers.size()))
            .replace("{max}", String.valueOf(ConfigManager.getRankedSize())));
    match.sendMessage(message);
    if (queuePlayers.size() >= ConfigManager.getRankedSize()) {
      startRanked(match);
    }
  }

  public static void removePlayer(MatchPlayer player) {
    UUID playerUUID = player.getId();
    if (!queuePlayers.contains(playerUUID)) {
      player.sendWarning(Component.text(LanguageManager.langMessage("ranked.prefix")
          + LanguageManager.langMessage("ranked.notInQueue")));
      return;
    }
    queuePlayers.remove(playerUUID);
    Component message = Component.text(LanguageManager.langMessage("ranked.prefix")
        + LanguageManager.langMessage("ranked.leftQueue")
            .replace("{player}", player.getPrefixedName())
            .replace("{size}", String.valueOf(queuePlayers.size()))
            .replace("{max}", String.valueOf(ConfigManager.getRankedSize())));
    player.getMatch().sendMessage(message);
  }

  public void startRanked(Match match) {
    if (match.getPhase() == MatchPhase.RUNNING
        || match.getPhase() == MatchPhase.FINISHED
        || countdownActive) {
      return;
    }
    countdownActive = true;
    final int[] countdown = {5};
    new BukkitRunnable() {
      @Override
      public void run() {
        if (countdown[0] <= 0) {
          String table = ConfigManager.getRankedDefaultTable();
          ConfigManager.addTempTable(table);
          queue(match, table);
          // Cancelar el countdown
          this.cancel();
          countdownActive = false;
          ranked = true;
          return;
        }
        if (queuePlayers.size() < ConfigManager.getRankedSize()) {
          this.cancel();
          countdownActive = false;
          match.sendWarning(Component.text(LanguageManager.langMessage("ranked.prefix")
              + LanguageManager.langMessage("ranked.cancelled")));
          return;
        }
        match.sendMessage(Component.text(LanguageManager.langMessage("ranked.prefix")
            + LanguageManager.langMessage("ranked.countdown")
                .replace("{time}", String.valueOf(countdown[0]))));
        match.playSound(Sounds.INVENTORY_CLICK);
        countdown[0]--;
      }
    }.runTaskTimer(plugin, 0, 20L);
  }

  private void queue(Match match, String table) {
    // Obtener a los primeros jugadores y borrarlos de la queue
    List<String> rankedPlayers = queuePlayers.subList(0, ConfigManager.getRankedSize()).stream()
        .map(uuid -> PGM.get().getMatchManager().getPlayer(uuid).getNameLegacy())
        .collect(Collectors.toList());
    // Obtener los MatchPlayers de los jugadores seleccionados
    List<MatchPlayer> rankedMatchPlayers = rankedPlayers.stream()
        .map(username -> PGM.get().getMatchManager().getPlayer(getUUIDFromUsername(username)))
        .collect(Collectors.toList());
    ItemListener.removeItemToPlayers(rankedMatchPlayers);
    // Borrar los jugadores de la cola
    queuePlayers.subList(0, ConfigManager.getRankedSize()).clear();
    // Obtener el elo
    StatsManager.getEloForUsernames(table, rankedPlayers)
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

          UUID captain1 = pair.getCaptain1();
          UUID captain2 = pair.getCaptain2();
          List<MatchPlayer> remaining = pair.getRemainingPlayers();

          if (ConfigManager.isRankedMatchmaking()) {
            matchmaking.startMatchmaking(captain1, captain2, rankedMatchPlayers, match);
          } else {
            draft.setCustomOrderPattern(ConfigManager.getRankedOrder(), 0);
            draft.startDraft(captain1, captain2, remaining, match, true);
          }
          rankedPlayers.clear();
          rankedMatchPlayers.clear();
          playersWithElo.clear();
        })
        .exceptionally(throwable -> {
          plugin
              .getLogger()
              .severe(LanguageManager.langMessage("ranked.error.getElo") + throwable.getMessage());
          match.sendWarning(Component.text(LanguageManager.langMessage("ranked.prefix")
              + LanguageManager.langMessage("ranked.error.getData")));
          return null;
        });
  }

  public List<String> getQueueList() {
    List<String> players = new ArrayList<>();
    for (UUID uuid : queuePlayers) {
      MatchPlayer player = PGM.get().getMatchManager().getPlayer(uuid);
      if (player != null) {
        String playerName = player.getPrefixedName();
        players.add(playerName);
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
