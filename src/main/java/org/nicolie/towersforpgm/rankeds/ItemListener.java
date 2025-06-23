package org.nicolie.towersforpgm.rankeds;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nicolie.towersforpgm.TowersForPGM;

import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class ItemListener implements Listener{
    private final Queue queue;

    public ItemListener(Queue queue) {
        this.queue = queue;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event){
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !isQueueItem(item)) {
            return;
        }
        event.setCancelled(true);
        Match match = PGM.get().getMatchManager().getMatch(event.getPlayer());
        MatchPlayer player = match.getPlayer(event.getPlayer());
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
        return displayName.equals(TowersForPGM.getInstance().getLanguageManager().getPluginMessage("ranked.item"));
    }
    
    public static ItemStack getQueueItem() {
        ItemStack queueItem = new ItemStack(Material.EYE_OF_ENDER);
        ItemMeta meta = queueItem.getItemMeta();
        meta.setDisplayName(TowersForPGM.getInstance().getLanguageManager().getPluginMessage("ranked.item"));
        queueItem.setItemMeta(meta);
        return queueItem;
    }

    public static void giveItem(MatchPlayer player) {
        if (!TowersForPGM.getInstance().getIsDatabaseActivated()){return;}
        player.getBukkit().getInventory().setItem(4, getQueueItem());
    }

    public static void giveItem(Player player) {
        if (!TowersForPGM.getInstance().getIsDatabaseActivated()){return;}
        player.getInventory().setItem(4, getQueueItem());
    }

    public static void giveItemToPlayers(Match match) {
        if (!TowersForPGM.getInstance().getIsDatabaseActivated()){return;}
        Bukkit.getScheduler().runTaskLater(
            TowersForPGM.getInstance(),
            () -> {
                for (MatchPlayer player : match.getPlayers()) {
                    giveItem(player);
                }
            },
            10L
        );
    }
}
