package org.nicolie.towersforpgm.rankeds;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.rankeds.listeners.RankedListener;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class ItemListener implements Listener {
  private final Queue queue;
  private static final Boolean RANKED_AVAILABLE =
      TowersForPGM.getInstance().getIsDatabaseActivated()
          || !ConfigManager.getRankedTables().isEmpty();

  private static final long QUEUE_COOLDOWN_MS = 15_000L;
  private static final Map<UUID, Long> queueCooldowns = new ConcurrentHashMap<>();

  public ItemListener(Queue queue) {
    this.queue = queue;
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR
        && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    ItemStack item = event.getItem();
    if (item == null || !isQueueItem(item)) {
      return;
    }
    event.setCancelled(true);
    Match match = PGM.get().getMatchManager().getMatch(event.getPlayer());
    MatchPlayer player = match.getPlayer(event.getPlayer());

    if (MatchBotConfig.isRankedEnabled()) {
      // Throttle requests per player
      UUID uuid = player.getId();
      long now = System.currentTimeMillis();
      long last = queueCooldowns.getOrDefault(uuid, 0L);
      if (now - last >= QUEUE_COOLDOWN_MS) {
        queueCooldowns.put(uuid, now);
        // Inform the player and trigger the Discord move
        player.sendMessage(Component.text(LanguageManager.langMessage("ranked.prefix")
            + LanguageManager.langMessage("ranked.queue.joining")));
        RankedListener.movePlayerToQueue(uuid);
      }
      return;
    }

    // Default behavior when bot is disabled: add to internal queue
    queue.addPlayer(player);
  }

  private boolean isQueueItem(ItemStack item) {
    if (item == null || item.getType() != Material.EYE_OF_ENDER) {
      return false;
    }
    ItemMeta meta = item.getItemMeta();
    if (meta == null || !meta.hasDisplayName()) {
      return false;
    }
    String displayName = meta.getDisplayName();
    return displayName.equals(LanguageManager.langMessage("ranked.item"));
  }

  public static ItemStack getQueueItem() {
    ItemStack queueItem = new ItemStack(Material.EYE_OF_ENDER);
    ItemMeta meta = queueItem.getItemMeta();
    meta.setDisplayName(LanguageManager.langMessage("ranked.item"));
    queueItem.setItemMeta(meta);
    return queueItem;
  }

  public static void giveRankedItem(MatchPlayer player) {
    if (!RANKED_AVAILABLE) return;
    if (MatchBotConfig.isRankedEnabled()) {
      // Give the item only after the match has finished
      Match match = player.getMatch();
      if (match != null && match.isFinished()) {
        player.getBukkit().getInventory().setItem(4, getQueueItem());
      }
      return;
    }
    // Bot disabled: current behavior
    player.getBukkit().getInventory().setItem(4, getQueueItem());
  }

  public static void giveItem(Player player) {
    if (!RANKED_AVAILABLE) return;
    if (MatchBotConfig.isRankedEnabled()) {
      Match match = PGM.get().getMatchManager().getMatch(player);
      if (match != null && match.isFinished()) {
        player.getInventory().setItem(4, getQueueItem());
      }
      return;
    }
    player.getInventory().setItem(4, getQueueItem());
  }



  public static void removeItemToPlayers(List<MatchPlayer> players) {
    if (!RANKED_AVAILABLE) return;
    for (MatchPlayer player : players) {
      player.getBukkit().getInventory().setItem(4, new ItemStack(Material.AIR));
    }
  }
}
