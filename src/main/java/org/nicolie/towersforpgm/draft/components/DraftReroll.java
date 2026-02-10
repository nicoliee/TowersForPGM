package org.nicolie.towersforpgm.draft.components;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.core.AvailablePlayers;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class DraftReroll {
  private static final Material REROLL_ITEM_MATERIAL = Material.BLAZE_POWDER;
  private static final int INITIAL_TIMER = 10;
  private static final int EXTENDED_TIMER = 15;
  public static final int CAPTAIN_PICK_TIMER = 35;
  public static final double MIN_VOTE_PERCENTAGE = 0.70;

  private final DraftDisplayManager displayManager;
  private final Captains captains;
  private final AvailablePlayers availablePlayers;

  private Set<UUID> rerollRequesters = new HashSet<>();
  private Set<UUID> eligibleVoters = new HashSet<>();
  private boolean timerExtended = false;
  private int[] remainingSeconds = {INITIAL_TIMER};
  private RerollCallback callback;

  public interface RerollCallback {
    void onRerollComplete(boolean approved);
  }

  public DraftReroll(
      TowersForPGM plugin,
      DraftState state,
      Captains captains,
      AvailablePlayers availablePlayers,
      Teams teams,
      DraftDisplayManager displayManager) {
    this.displayManager = displayManager;
    this.captains = captains;
    this.availablePlayers = availablePlayers;
  }

  public void startRerollPhase(Match match, RerollCallback callback) {
    this.callback = callback;
    reset();
    remainingSeconds[0] = INITIAL_TIMER;

    initializeEligibleVoters(match);
    giveRerollItem(match);
    match.sendActionBar(Component.text(LanguageManager.message("draft.reroll.actionBar")));
    startRerollTimer();
  }

  private void initializeEligibleVoters(Match match) {
    eligibleVoters.add(captains.getCaptain1());
    eligibleVoters.add(captains.getCaptain2());
    for (MatchPlayer mp : availablePlayers.getAvailablePlayers()) {
      eligibleVoters.add(mp.getId());
    }
  }

  private void startRerollTimer() {
    String baseMessage = LanguageManager.message("draft.reroll.bossbar.title");
    displayManager.startTimer(
        remainingSeconds[0],
        baseMessage.replace("{time}", DraftDisplayManager.formatTime(remainingSeconds[0])),
        BossBar.Color.YELLOW,
        () -> {
          remainingSeconds[0]--;
          displayManager.updateBarMessage(
              baseMessage.replace("{time}", DraftDisplayManager.formatTime(remainingSeconds[0])));
        },
        () -> completeReroll(false));
  }

  public boolean onPlayerRequestReroll(UUID playerId) {
    if (!eligibleVoters.contains(playerId) || rerollRequesters.contains(playerId)) return false;

    rerollRequesters.add(playerId);

    MatchPlayer player = PGM.get().getMatchManager().getPlayer(playerId);
    int minRequired = getMinimumRerollVotes();
    int current = rerollRequesters.size();

    MatchManager.getMatch()
        .sendWarning(Component.text(LanguageManager.message("draft.reroll.requested")
            .replace("{player}", player != null ? player.getPrefixedName() : "Player")
            .replace("{current}", String.valueOf(current))
            .replace("{min}", String.valueOf(minRequired))));

    if (!timerExtended && current == 1) {
      timerExtended = true;
      remainingSeconds[0] = EXTENDED_TIMER;
      displayManager.cancelTimer();
      startRerollTimer();
    }

    if (current >= minRequired) completeReroll(true);
    return true;
  }

  private void completeReroll(boolean approved) {
    displayManager.cancelTimer();
    if (approved) {
      MatchManager.getMatch()
          .sendWarning(Component.text(LanguageManager.message("draft.reroll.approved")));
    }
    if (callback != null) callback.onRerollComplete(approved);
  }

  private int getMinimumRerollVotes() {
    return (int) Math.ceil(eligibleVoters.size() * MIN_VOTE_PERCENTAGE);
  }

  public static boolean isRerollItem(ItemStack item) {
    if (item == null || item.getType() != REROLL_ITEM_MATERIAL) return false;
    ItemMeta meta = item.getItemMeta();
    return meta != null
        && meta.hasDisplayName()
        && meta.getDisplayName().equals(LanguageManager.message("draft.reroll.item.name"));
  }

  public void cancel() {
    displayManager.cancelTimer();
    reset();
  }

  private void reset() {
    rerollRequesters.clear();
    eligibleVoters.clear();
    timerExtended = false;
  }

  public Set<UUID> getRerollRequesters() {
    return new HashSet<>(rerollRequesters);
  }

  private void giveRerollItem(Match match) {
    ItemStack rerollItem = createRerollItem();
    for (MatchPlayer mp : match.getPlayers()) {
      if (eligibleVoters.contains(mp.getId())) {
        Player player = mp.getBukkit();
        if (player != null && player.isOnline()) {
          player.getInventory().setItem(2, rerollItem);
        }
      }
    }
  }

  private ItemStack createRerollItem() {
    ItemStack item = new ItemStack(REROLL_ITEM_MATERIAL);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(LanguageManager.message("draft.reroll.item.name"));
      List<String> lore = LanguageManager.messageList("draft.reroll.item.lore");
      List<String> processedLore = new ArrayList<>();
      for (String line : lore) {
        processedLore.add(
            line.replace("{percentage}", String.valueOf((int) (MIN_VOTE_PERCENTAGE * 100))));
      }
      meta.setLore(processedLore);
      item.setItemMeta(meta);
    }
    return item;
  }
}
