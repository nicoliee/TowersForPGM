package org.nicolie.towersforpgm.draft.pick.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.draft.state.DraftPhase;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.text.TextTranslations;

public class PicksGUIManager implements Listener {

  private static final int HOTBAR_SLOT = 2;

  private final TowersForPGM plugin;

  public PicksGUIManager(TowersForPGM plugin) {
    this.plugin = plugin;
  }

  public void openMenu(MatchPlayer viewer) {
    Match match = viewer.getMatch();
    DraftContext ctx = getContext(match);
    if (ctx == null) return;
    new PicksMenu(viewer, plugin, ctx).open();
  }

  public static void giveItem(Match match) {
    match.getPlayers().forEach(mp -> giveItem(mp.getBukkit()));
  }

  @SuppressWarnings("deprecation")
  public static void giveItem(Player player) {
    ItemStack item = new ItemStack(Material.NETHER_STAR);
    ItemMeta meta = item.getItemMeta();
    Component displayNameComponent =
        Component.translatable("draft.item").color(NamedTextColor.GOLD);
    String displayName = TextTranslations.translateLegacy(displayNameComponent, player);
    meta.setDisplayName(displayName);
    item.setItemMeta(meta);
    player.getInventory().setItem(HOTBAR_SLOT, null);
    player.getInventory().setItem(HOTBAR_SLOT, item);
  }

  public static void removeItem(Match match) {
    match.getPlayers().forEach(mp -> removeItem(mp.getBukkit()));
  }

  public static void removeItem(Player player) {
    for (ItemStack item : player.getInventory().getContents()) {
      if (isDraftItem(item, player)) {
        player.getInventory().remove(item);
      }
    }
  }

  @SuppressWarnings("deprecation")
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR
        && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

    Player player = event.getPlayer();

    ItemStack item;
    try {
      item = player.getInventory().getItemInMainHand();
    } catch (NoSuchMethodError e) {
      item = player.getItemInHand();
    }

    if (!isDraftItem(item, player)) return;

    Match match = PGM.get().getMatchManager().getMatch(player);
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    DraftContext ctx = getContext(match);

    if (ctx == null) {
      matchPlayer.sendWarning(Component.translatable("draft.inactive"));
      player.getInventory().remove(Material.NETHER_STAR);
      return;
    }

    openMenu(matchPlayer);
    event.setCancelled(true);
  }

  private DraftContext getContext(Match match) {
    if (match == null) return null;
    var session = MatchSessionRegistry.get(match);
    if (session == null) return null;
    DraftContext ctx = session.getDraft();
    if (ctx == null || ctx.phase() == DraftPhase.IDLE || ctx.phase() == DraftPhase.ENDED)
      return null;
    return ctx;
  }

  @SuppressWarnings("deprecation")
  private static boolean isDraftItem(ItemStack item, Player player) {
    Component displayNameComponent =
        Component.translatable("draft.item").color(NamedTextColor.GOLD);
    String expected = TextTranslations.translateLegacy(displayNameComponent, player);
    return item != null
        && item.getType() == Material.NETHER_STAR
        && item.hasItemMeta()
        && expected.equals(item.getItemMeta().getDisplayName());
  }
}
