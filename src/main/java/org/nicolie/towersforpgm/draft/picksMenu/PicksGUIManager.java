package org.nicolie.towersforpgm.draft.picksMenu;

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
import org.nicolie.towersforpgm.draft.components.DraftPhase;
import org.nicolie.towersforpgm.draft.core.AvailablePlayers;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Draft;
import org.nicolie.towersforpgm.draft.core.Teams;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

/**
 * Handles the hotbar item (Nether Star) that opens PicksMenu, and exposes helpers for
 * giving/removing the item and updating open menus.
 */
public class PicksGUIManager implements Listener {

  private static final String ITEM_NAME = "§6Draft Menu";
  private static final int HOTBAR_SLOT = 2;

  private final TowersForPGM plugin;
  private final Draft draft;
  private final Captains captains;
  private final AvailablePlayers availablePlayers;
  private final Teams teams;

  public PicksGUIManager(
      TowersForPGM plugin,
      Draft draft,
      Captains captains,
      AvailablePlayers availablePlayers,
      Teams teams) {
    this.plugin = plugin;
    this.draft = draft;
    this.captains = captains;
    this.availablePlayers = availablePlayers;
    this.teams = teams;
  }

  public void openMenu(MatchPlayer viewer) {
    new PicksMenu(viewer, plugin, draft, captains, availablePlayers, teams).open();
  }

  public static void giveItem(Match match) {
    match.getPlayers().forEach(mp -> giveItem(mp.getBukkit()));
  }

  public static void giveItem(Player player) {
    ItemStack item = new ItemStack(Material.NETHER_STAR);
    ItemMeta meta = item.getItemMeta();
    meta.setDisplayName(ITEM_NAME);
    item.setItemMeta(meta);

    player.getInventory().setItem(HOTBAR_SLOT, null);
    player.getInventory().setItem(HOTBAR_SLOT, item);
  }

  public static void removeItem(Match match) {
    match.getPlayers().forEach(mp -> removeItem(mp.getBukkit()));
  }

  public static void removeItem(Player player) {
    for (ItemStack item : player.getInventory().getContents()) {
      if (item != null && item.getType() == Material.NETHER_STAR && item.hasItemMeta()) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && ITEM_NAME.equals(meta.getDisplayName())) {
          player.getInventory().remove(item);
        }
      }
    }
  }

  private boolean isDraftItem(ItemStack item) {
    return item != null
        && item.getType() == Material.NETHER_STAR
        && item.hasItemMeta()
        && ITEM_NAME.equals(item.getItemMeta().getDisplayName());
  }

  @SuppressWarnings("deprecation")
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    if (event.getAction() != Action.RIGHT_CLICK_AIR
        && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

    ItemStack item;
    try {
      item = player.getInventory().getItemInMainHand();
    } catch (NoSuchMethodError e) {
      item = player.getItemInHand();
    }

    if (!isDraftItem(item)) return;

    if (Draft.getPhase() == DraftPhase.IDLE || Draft.getPhase() == DraftPhase.ENDED) {
      MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
      matchPlayer.sendWarning(Component.translatable("draft.inactive"));
      player.getInventory().remove(Material.NETHER_STAR);
      return;
    }

    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    openMenu(matchPlayer);
    event.setCancelled(true);
  }
}
