package org.nicolie.towersforpgm.draft.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.map.gui.MapVoteMenu;
import org.nicolie.towersforpgm.draft.map.modes.AutomaticMode;
import org.nicolie.towersforpgm.draft.map.modes.PluralityMode;
import org.nicolie.towersforpgm.draft.map.modes.VetoMode;
import org.nicolie.towersforpgm.draft.team.AvailablePlayers;
import org.nicolie.towersforpgm.draft.team.Captains;
import org.nicolie.towersforpgm.draft.timer.BossbarTimer;
import org.nicolie.towersforpgm.draft.timer.MapVoteTimer;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.text.TextTranslations;

public final class MapVoteManager {

  public interface VoteCallback {
    void onVoteComplete(String winningMap);
  }

  private static final int VOTE_ITEM_SLOT = 2;

  public static final String SECRET_KEY = "__SECRET__";

  private final List<String> displayedMaps = new ArrayList<>();
  private final List<String> remainingMaps = new ArrayList<>();
  private final Map<UUID, String> votes = new HashMap<>();
  private final Set<UUID> eligibleVoters = new LinkedHashSet<>();
  private final Map<UUID, MapVoteMenu> openMenus = new HashMap<>();

  private String secretMapName = null;
  private VoteCallback callback;
  private boolean finished = false;
  private PluralityMode pluralityMode;
  private VetoMode vetoMode;
  private AutomaticMode automaticMode;

  private final Match match;
  private final MapVoteConfig config;
  private final MapVoteTimer mapVoteTimer;
  private final Captains captains;
  private final AvailablePlayers availablePlayers;

  public MapVoteManager(
      Match match,
      TowersForPGM plugin,
      MapVoteConfig config,
      Captains captains,
      AvailablePlayers availablePlayers,
      BossbarTimer bossbarTimer) {
    this.match = match;
    this.config = config;
    this.mapVoteTimer = new MapVoteTimer(bossbarTimer, config);
    this.captains = captains;
    this.availablePlayers = availablePlayers;
  }

  public void startVote(VoteCallback callback) {
    this.callback = callback;
    this.finished = false;

    buildDisplayedMaps();
    initModes();

    remainingMaps.clear();
    remainingMaps.addAll(
        displayedMaps.stream().filter(m -> !SECRET_KEY.equals(m)).collect(Collectors.toList()));

    buildEligibleVoters();
    giveVoteItemToAll();
    showStartTitle();
    startBossbarTimer();
  }

  public void castVote(UUID voterUUID, String displayedMapName) {
    if (finished) return;
    if (!eligibleVoters.contains(voterUUID)) return;

    switch (config.getVoteMode()) {
      case VETO:
        vetoMode.castVetoVote(voterUUID, displayedMapName);
        // Sincronizar remainingMaps desde el mode (VetoMode los modifica internamente)
        syncRemainingMapsFromVeto();

        Player p = Bukkit.getPlayer(voterUUID);
        if (p != null) {
          match.sendMessage(Component.translatable(
                  "draft.map.veto",
                  Component.text(p.getName()).color(NamedTextColor.AQUA),
                  Component.text(displayedMapName).color(NamedTextColor.RED))
              .color(NamedTextColor.GRAY));
        }
        if (remainingMaps.size() == 1) finish();
        break;

      case PLURALITY:
      default:
        pluralityMode.castPluralityVote(voterUUID, displayedMapName);
        votes.put(
            voterUUID, displayedMapName); // mantener espejo para getVoteCounts/getVotersForMap
        if (votes.size() >= eligibleVoters.size()) finish();
        break;
    }
  }

  public List<String> getDisplayedMaps() {
    return Collections.unmodifiableList(displayedMaps);
  }

  public List<String> getRemainingMaps() {
    return Collections.unmodifiableList(remainingMaps);
  }

  public String getCurrentVote(UUID uuid) {
    return votes.get(uuid);
  }

  public boolean isEligible(UUID uuid) {
    return eligibleVoters.contains(uuid);
  }

  public boolean hasVoted(UUID uuid) {
    return votes.containsKey(uuid);
  }

  public MapVoteConfig getConfig() {
    return config;
  }

  public int getTimeLeft() {
    return mapVoteTimer.getTimeLeft();
  }

  public String getSecretMapName() {
    return secretMapName;
  }

  public Map<String, Integer> getVoteCounts() {
    Map<String, Integer> counts = new HashMap<>();
    displayedMaps.forEach(m -> counts.put(m, 0));
    votes.values().forEach(m -> counts.merge(m, 1, Integer::sum));
    return counts;
  }

  public List<String> getVotersForMap(String displayedName) {
    return votes.entrySet().stream()
        .filter(e -> e.getValue().equals(displayedName))
        .map(e -> {
          Player p = Bukkit.getPlayer(e.getKey());
          return p != null ? p.getName() : null;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public void cancel() {
    finished = true;
    mapVoteTimer.cancel();
    closeAllMenus();
    removeVoteItemFromAll();
  }

  public static ItemStack createVoteItem(Player player) {
    ItemStack item = new ItemStack(Material.NETHER_STAR);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      Component displayNameComponent =
          Component.translatable("draft.map.vote.title").color(NamedTextColor.GOLD);
      @SuppressWarnings("deprecation")
      String displayName = TextTranslations.translateLegacy(displayNameComponent, player);
      meta.setDisplayName(displayName);
      item.setItemMeta(meta);
    }
    return item;
  }

  public static boolean isVoteItem(ItemStack item, Player player) {
    if (item == null || item.getType() != Material.NETHER_STAR) return false;
    ItemMeta meta = item.getItemMeta();
    if (meta == null || !meta.hasDisplayName()) return false;
    Component displayNameComponent =
        Component.translatable("draft.map.vote.title").color(NamedTextColor.GOLD);
    @SuppressWarnings("deprecation")
    String expected = TextTranslations.translateLegacy(displayNameComponent, player);
    return expected.equals(meta.getDisplayName());
  }

  public void giveVoteItem(Player player) {
    player.getInventory().setItem(VOTE_ITEM_SLOT, createVoteItem(player));
  }

  public void openMenuFor(MatchPlayer mp) {
    MapVoteMenu menu = new MapVoteMenu(mp, this);
    openMenus.put(mp.getId(), menu);
    menu.open();
  }

  private void initModes() {
    switch (config.getVoteMode()) {
      case PLURALITY:
        pluralityMode = new PluralityMode(config, displayedMaps, votes, SECRET_KEY, secretMapName);
        break;
      case VETO:
        vetoMode = new VetoMode(config, displayedMaps, remainingMaps, votes, SECRET_KEY);
        break;
      case AUTOMATIC:
        automaticMode = new AutomaticMode(config.getMaps());
        break;
      default:
        break;
    }
  }

  private void syncRemainingMapsFromVeto() {
    remainingMaps.clear();
    for (String mapName : displayedMaps) {
      if (SECRET_KEY.equals(mapName)) continue;
      if (!votes.containsValue(mapName)) remainingMaps.add(mapName);
    }
  }

  private void giveVoteItemToAll() {
    match.getPlayers().forEach(mp -> giveVoteItem(mp.getBukkit()));
  }

  private void removeVoteItemFromAll() {
    for (MatchPlayer mp : match.getPlayers()) {
      Player p = mp.getBukkit();
      ItemStack current = p.getInventory().getItem(VOTE_ITEM_SLOT);
      if (isVoteItem(current, p)) p.getInventory().setItem(VOTE_ITEM_SLOT, null);
    }
  }

  private void showStartTitle() {
    match.getPlayers().forEach(mp -> mp.showTitle(buildTitleForPlayer(mp.getBukkit())));
  }

  private Title buildTitleForPlayer(Player player) {
    Component titleText = Component.translatable(titleKeyForVoteMode())
        .color(NamedTextColor.AQUA)
        .decorate(TextDecoration.BOLD);
    Component subtitleText =
        Component.translatable(subtitleKeyForCurrentModes(player)).color(NamedTextColor.GRAY);
    return Title.title(
        titleText,
        subtitleText,
        Title.Times.times(
            java.time.Duration.ofMillis(500),
            java.time.Duration.ofSeconds(4),
            java.time.Duration.ofMillis(500)));
  }

  private String titleKeyForVoteMode() {
    switch (config.getVoteMode()) {
      case VETO:
        return "draft.map.veto.title";
      case PLURALITY:
      default:
        return "draft.map.vote.title";
    }
  }

  private String subtitleKeyForCurrentModes(Player player) {
    boolean canVote = eligibleVoters.contains(player.getUniqueId());
    switch (config.getVoteMode()) {
      case VETO:
        return canVote ? "draft.map.veto.subtitle.canvote" : "draft.map.subtitle.cannotvote";
      case PLURALITY:
      default:
        return canVote ? "draft.map.vote.subtitle.canvote" : "draft.map.subtitle.cannotvote";
    }
  }

  private void startBossbarTimer() {
    mapVoteTimer.start(this::finish);
  }

  private void buildEligibleVoters() {
    eligibleVoters.clear();
    switch (config.getVoterMode()) {
      case ALL:
        eligibleVoters.add(captains.getCaptain1());
        eligibleVoters.add(captains.getCaptain2());
        availablePlayers.getAvailablePlayers().forEach(mp -> eligibleVoters.add(mp.getId()));
        break;
      case CAPTAINS_ONLY:
        eligibleVoters.add(captains.getCaptain1());
        eligibleVoters.add(captains.getCaptain2());
        break;
      case PLAYERS_ONLY:
        availablePlayers.getAvailablePlayers().forEach(mp -> eligibleVoters.add(mp.getId()));
        break;
    }
    eligibleVoters.remove(null);
  }

  private void closeAllMenus() {
    new HashSet<>(openMenus.keySet()).forEach(uuid -> {
      Player p = Bukkit.getPlayer(uuid);
      if (p != null) p.closeInventory();
    });
    openMenus.clear();
  }

  private void finish() {
    if (finished) return;
    finished = true;
    mapVoteTimer.cancel();
    closeAllMenus();
    removeVoteItemFromAll();

    String winner = resolveWinner();
    if (callback != null) callback.onVoteComplete(winner);
  }

  private String resolveWinner() {
    switch (config.getVoteMode()) {
      case VETO:
        return vetoMode.resolveVetoWinner();
      case AUTOMATIC:
        return automaticMode.resolveWinner();
      case PLURALITY:
      default:
        return pluralityMode.resolvePluralityWinner();
    }
  }

  private void buildDisplayedMaps() {
    displayedMaps.clear();
    secretMapName = null;
    List<String> all = new ArrayList<>(config.getMaps());

    switch (config.getVoteMode()) {
      case VETO:
      case AUTOMATIC:
        // En VETO se muestran todos; en AUTOMATIC no importa (no hay GUI),
        // pero rellenamos igual para que resolvePluralityWinner tenga contexto si se reutiliza
        displayedMaps.addAll(all);
        return;
      case PLURALITY:
      default:
        break;
    }

    if (all.size() <= 3) {
      displayedMaps.addAll(all);
      return;
    }

    List<String> pool = new ArrayList<>(all);
    Random rng = new Random();

    String currentMap = match.getMap().getName();
    String slot1 = pool.contains(currentMap) ? currentMap : pool.get(rng.nextInt(pool.size()));
    pool.remove(slot1);
    displayedMaps.add(slot1);

    String slot2 = pool.get(rng.nextInt(pool.size()));
    pool.remove(slot2);
    displayedMaps.add(slot2);

    secretMapName = pool.get(rng.nextInt(pool.size()));
    displayedMaps.add(SECRET_KEY);
  }
}
