package org.nicolie.towersforpgm.draft.components;

import java.util.*;
import java.util.stream.Collectors;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.draft.core.AvailablePlayers;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public class RerollOptionsGUI implements Listener {
  private static final int MIN_VOTES_FAST_TIMER = 5;
  private static String GUI_TITLE;

  private final DraftDisplayManager displayManager;
  private final AvailablePlayers availablePlayers;
  private final Teams teams;

  private List<CaptainPair> options = new ArrayList<>();
  private Map<UUID, Integer> votes = new HashMap<>();
  private Set<UUID> playersWithGuiOpen = new HashSet<>();
  private Set<CaptainPair> usedPairs = new HashSet<>();
  private int[] remainingSeconds = {DraftReroll.CAPTAIN_PICK_TIMER};
  private int totalEligibleVoters;
  private boolean timerReduced = false;
  private RerollOptionsCallback callback;
  private Match currentMatch;

  /** Represents a pair of captains with their statistics. */
  public static class CaptainPair {
    public final String captain1;
    public final String captain2;
    public final Stats stats1;
    public final Stats stats2;

    public CaptainPair(String captain1, String captain2, Stats stats1, Stats stats2) {
      this.captain1 = captain1;
      this.captain2 = captain2;
      this.stats1 = stats1;
      this.stats2 = stats2;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof CaptainPair)) return false;
      CaptainPair other = (CaptainPair) obj;
      return (captain1.equals(other.captain1) && captain2.equals(other.captain2))
          || (captain1.equals(other.captain2) && captain2.equals(other.captain1));
    }

    @Override
    public int hashCode() {
      return captain1.hashCode() + captain2.hashCode();
    }
  }

  public interface RerollOptionsCallback {
    void onVotingComplete(CaptainPair selectedPair);
  }

  public RerollOptionsGUI(
      TowersForPGM plugin,
      AvailablePlayers availablePlayers,
      Teams teams,
      DraftDisplayManager displayManager) {
    this.displayManager = displayManager;
    this.availablePlayers = availablePlayers;
    this.teams = teams;
  }

  public void startVoting(
      Match match,
      String originalCaptain1,
      String originalCaptain2,
      RerollOptionsCallback callback) {
    GUI_TITLE = LanguageManager.message("draft.reroll.voting.title");
    this.callback = callback;
    this.currentMatch = match;
    reset();
    this.totalEligibleVoters = match.getPlayers().size();
    this.remainingSeconds[0] = DraftReroll.CAPTAIN_PICK_TIMER;

    usedPairs.add(new CaptainPair(originalCaptain1, originalCaptain2, null, null));
    generateOptions(match);

    for (MatchPlayer mp : match.getPlayers()) {
      Player player = mp.getBukkit();
      if (player != null && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
        openVotingGUI(player);
      }
    }

    startVotingTimer();
  }

  private void reset() {
    votes.clear();
    playersWithGuiOpen.clear();
    usedPairs.clear();
    timerReduced = false;
  }

  private void generateOptions(Match match) {
    options.clear();

    List<PlayerWithStats> players = new ArrayList<>();
    for (MatchPlayer mp : availablePlayers.getAvailablePlayers()) {
      Player player = mp.getBukkit();
      if (player != null && player.isOnline()) {
        Stats stats = availablePlayers.getStatsForPlayer(player.getName());
        if (stats != null) {
          players.add(new PlayerWithStats(player.getName(), stats));
        }
      }
    }

    if (players.size() < 2) {
      options.add(selectRandomPair(players));
      options.add(selectRandomPair(players));
      options.add(selectRandomPair(players));
      return;
    }

    CaptainPair pointsCaptains = selectByAveragePoints(players);
    CaptainPair killsCaptains = selectByAverageKills(players);

    options.add(pointsCaptains);
    options.add(killsCaptains);
    options.add(selectRandomPairExcluding(players, pointsCaptains, killsCaptains));
  }

  private CaptainPair selectByAveragePoints(List<PlayerWithStats> players) {
    double avgPoints = players.stream()
        .mapToDouble(p -> (double) p.stats.getPoints())
        .average()
        .orElse(0);
    return selectPairClosestToValue(players, avgPoints, p -> (double) p.stats.getPoints());
  }

  private CaptainPair selectByAverageKills(List<PlayerWithStats> players) {
    double avgKills =
        players.stream().mapToDouble(p -> (double) p.stats.getKills()).average().orElse(0);
    return selectPairClosestToValue(players, avgKills, p -> (double) p.stats.getKills());
  }

  private CaptainPair selectPairClosestToValue(
      List<PlayerWithStats> players,
      double targetValue,
      java.util.function.Function<PlayerWithStats, Double> valueExtractor) {
    List<PlayerWithStats> sorted = players.stream()
        .sorted(Comparator.comparingDouble(p -> Math.abs(valueExtractor.apply(p) - targetValue)))
        .collect(Collectors.toList());

    for (int i = 0; i < sorted.size() - 1; i++) {
      CaptainPair pair = new CaptainPair(
          sorted.get(i).name, sorted.get(i + 1).name,
          sorted.get(i).stats, sorted.get(i + 1).stats);
      if (!usedPairs.contains(pair)) {
        usedPairs.add(pair);
        return pair;
      }
    }

    if (sorted.size() >= 2) {
      CaptainPair pair = new CaptainPair(
          sorted.get(0).name, sorted.get(1).name, sorted.get(0).stats, sorted.get(1).stats);
      usedPairs.add(pair);
      return pair;
    }
    return selectRandomPair(players);
  }

  private CaptainPair selectRandomPair(List<PlayerWithStats> players) {
    if (players.size() < 2) return null;

    Random random = new Random();
    for (int attempt = 0; attempt < 50; attempt++) {
      int idx1 = random.nextInt(players.size());
      int idx2 = random.nextInt(players.size());

      if (idx1 != idx2) {
        CaptainPair pair = new CaptainPair(
            players.get(idx1).name, players.get(idx2).name,
            players.get(idx1).stats, players.get(idx2).stats);
        if (!usedPairs.contains(pair)) {
          usedPairs.add(pair);
          return pair;
        }
      }
    }

    CaptainPair pair = new CaptainPair(
        players.get(0).name, players.get(1).name,
        players.get(0).stats, players.get(1).stats);
    usedPairs.add(pair);
    return pair;
  }

  private CaptainPair selectRandomPairExcluding(
      List<PlayerWithStats> players, CaptainPair pointsCaptains, CaptainPair killsCaptains) {
    if (players.size() < 2) return selectRandomPair(players);

    Set<String> excludedNames = new HashSet<>();
    if (pointsCaptains != null) {
      excludedNames.add(pointsCaptains.captain1);
      excludedNames.add(pointsCaptains.captain2);
    }
    if (killsCaptains != null) {
      excludedNames.add(killsCaptains.captain1);
      excludedNames.add(killsCaptains.captain2);
    }

    List<PlayerWithStats> availablePlayers =
        players.stream().filter(p -> !excludedNames.contains(p.name)).collect(Collectors.toList());

    if (availablePlayers.size() < 2) {
      return selectRandomPair(players);
    }

    // Seleccionar aleatoriamente de los jugadores disponibles
    Random random = new Random();
    for (int attempt = 0; attempt < 50; attempt++) {
      int idx1 = random.nextInt(availablePlayers.size());
      int idx2 = random.nextInt(availablePlayers.size());

      if (idx1 != idx2) {
        CaptainPair pair = new CaptainPair(
            availablePlayers.get(idx1).name, availablePlayers.get(idx2).name,
            availablePlayers.get(idx1).stats, availablePlayers.get(idx2).stats);
        if (!usedPairs.contains(pair)) {
          usedPairs.add(pair);
          return pair;
        }
      }
    }

    // Si no se pudo encontrar un par no usado, devolver el primero disponible
    CaptainPair pair = new CaptainPair(
        availablePlayers.get(0).name, availablePlayers.get(1).name,
        availablePlayers.get(0).stats, availablePlayers.get(1).stats);
    usedPairs.add(pair);
    return pair;
  }

  public void reopenGUI(Player player) {
    if (player != null && player.isOnline()) {
      openVotingGUI(player);
    }
  }

  private void openVotingGUI(Player player) {
    Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);

    ItemStack grayGlass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
    ItemMeta glassMeta = grayGlass.getItemMeta();
    if (glassMeta != null) {
      glassMeta.setDisplayName(" ");
      grayGlass.setItemMeta(glassMeta);
    }
    for (int i = 0; i < 27; i++) {
      inv.setItem(i, grayGlass);
    }

    if (options.size() >= 1 && options.get(0) != null) {
      inv.setItem(
          10,
          createOptionItem(
              0, options.get(0), LanguageManager.message("draft.reroll.voting.option1")));
    }
    if (options.size() >= 2 && options.get(1) != null) {
      inv.setItem(
          13,
          createOptionItem(
              1, options.get(1), LanguageManager.message("draft.reroll.voting.option2")));
    }
    if (options.size() >= 3 && options.get(2) != null) {
      inv.setItem(
          16,
          createOptionItem(
              2, options.get(2), LanguageManager.message("draft.reroll.voting.option3")));
    }

    player.openInventory(inv);
    playersWithGuiOpen.add(player.getUniqueId());
  }

  private ItemStack createOptionItem(int optionIndex, CaptainPair pair, String title) {
    Material material;
    short data = 0;

    switch (optionIndex) {
      case 0:
        material = Material.EMERALD;
        break;
      case 1:
        material = Material.IRON_SWORD;
        break;
      case 2:
        material = Material.PAPER;
        break;
      default:
        material = Material.SKULL_ITEM;
        data = 3;
    }

    ItemStack item = data > 0 ? new ItemStack(material, 1, data) : new ItemStack(material);
    ItemMeta meta = item.getItemMeta();

    if (meta != null) {
      if (meta instanceof SkullMeta && optionIndex < 3) {
        ((SkullMeta) meta).setOwner(pair.captain1);
      }
      meta.setDisplayName(title);

      List<String> lore = new ArrayList<>();
      lore.add("§7");
      lore.add(teams.getTeamColor(1) + pair.captain1 + " §l§bvs. " + teams.getTeamColor(2)
          + pair.captain2);
      lore.add("§7");
      lore.add("§7");

      List<String> voters = votes.entrySet().stream()
          .filter(e -> e.getValue() == optionIndex)
          .map(e -> {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null) {
              MatchPlayer mp = PGM.get().getMatchManager().getPlayer(p);
              if (mp != null) return mp.getPrefixedName();
            }
            return null;
          })
          .filter(Objects::nonNull)
          .collect(Collectors.toList());

      if (!voters.isEmpty()) {
        lore.add(LanguageManager.message("draft.reroll.voting.votes.header"));
        voters.forEach(voter -> lore.add("  " + voter));
      } else {
        lore.add(LanguageManager.message("draft.reroll.voting.votes.none"));
      }
      lore.add("§7");
      lore.add(LanguageManager.message("draft.reroll.voting.clickToVote"));

      meta.setLore(lore);
      item.setItemMeta(meta);
    }
    return item;
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) return;
    if (GUI_TITLE == null || !event.getView().getTitle().equals(GUI_TITLE)) return;

    event.setCancelled(true);
    Player player = (Player) event.getWhoClicked();
    int slot = event.getSlot();

    Integer optionIndex = null;
    if (slot == 10) optionIndex = 0;
    else if (slot == 13) optionIndex = 1;
    else if (slot == 16) optionIndex = 2;

    if (optionIndex != null && optionIndex < options.size()) {
      votes.put(player.getUniqueId(), optionIndex);
      MatchPlayer mp = PGM.get().getMatchManager().getPlayer(player);
      if (mp != null) mp.playSound(Sounds.INVENTORY_CLICK);
      refreshAllGUIs();

      if (votes.size() >= totalEligibleVoters) {
        completeVoting();
      }
    }
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (GUI_TITLE != null && event.getView().getTitle().equals(GUI_TITLE)) {
      playersWithGuiOpen.remove(event.getPlayer().getUniqueId());
    }
  }

  private void refreshAllGUIs() {
    for (UUID playerId : new HashSet<>(playersWithGuiOpen)) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null && player.isOnline()) {
        openVotingGUI(player);
      }
    }
  }

  private void startVotingTimer() {
    String baseMessage = LanguageManager.message("draft.reroll.voting.bossbar");
    displayManager.startTimer(
        remainingSeconds[0],
        formatMessage(baseMessage),
        BossBar.Color.GREEN,
        () -> {
          checkFastTimer();
          remainingSeconds[0]--;
          displayManager.updateBarMessage(formatMessage(baseMessage));
        },
        this::completeVoting);
  }

  private void checkFastTimer() {
    if (!timerReduced && totalEligibleVoters > 0) {
      int minVotesRequired = (int) Math.ceil(totalEligibleVoters * DraftReroll.MIN_VOTE_PERCENTAGE);
      if (votes.size() >= minVotesRequired && remainingSeconds[0] > MIN_VOTES_FAST_TIMER) {
        timerReduced = true;
        remainingSeconds[0] = MIN_VOTES_FAST_TIMER;
        displayManager.cancelTimer();
        startVotingTimer();
      }
    }
  }

  private String formatMessage(String baseMessage) {
    return baseMessage
        .replace("{time}", DraftDisplayManager.formatTime(remainingSeconds[0]))
        .replace("{current}", String.valueOf(votes.size()))
        .replace("{total}", String.valueOf(totalEligibleVoters));
  }

  private void completeVoting() {
    displayManager.cancelTimer();

    for (UUID playerId : new HashSet<>(playersWithGuiOpen)) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null && player.isOnline()) {
        player.closeInventory();
      }
    }
    playersWithGuiOpen.clear();

    if (currentMatch != null) {
      for (MatchPlayer mp : currentMatch.getPlayers()) {
        Player player = mp.getBukkit();
        if (player != null && player.isOnline()) {
          player.getInventory().remove(Material.BLAZE_POWDER);
        }
      }
    }

    if (callback != null) {
      callback.onVotingComplete(determineWinner());
    }
  }

  private CaptainPair determineWinner() {
    if (votes.isEmpty()) {
      return options.isEmpty() ? null : options.get(new Random().nextInt(options.size()));
    }

    Map<Integer, Long> voteCounts =
        votes.values().stream().collect(Collectors.groupingBy(v -> v, Collectors.counting()));

    int winningOption = voteCounts.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse(0);

    return winningOption < options.size() ? options.get(winningOption) : null;
  }

  public void cancel() {
    displayManager.cancelTimer();
    for (UUID playerId : new HashSet<>(playersWithGuiOpen)) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null && player.isOnline()) {
        player.closeInventory();
      }
    }
    playersWithGuiOpen.clear();
    votes.clear();
    options.clear();
  }

  private static class PlayerWithStats {
    final String name;
    final Stats stats;

    PlayerWithStats(String name, Stats stats) {
      this.name = name;
      this.stats = stats;
    }
  }
}
