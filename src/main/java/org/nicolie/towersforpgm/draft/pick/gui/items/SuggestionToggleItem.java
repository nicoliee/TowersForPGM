package org.nicolie.towersforpgm.draft.pick.gui.items;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class SuggestionToggleItem implements MenuItem {

  private final DraftContext ctx;

  public SuggestionToggleItem(DraftContext ctx) {
    this.ctx = ctx;
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("draft.gui.suggestion").color(NamedTextColor.GREEN);
  }

  @SuppressWarnings("deprecation")
  @Override
  public List<String> getLore(Player player) {
    return List.of(TextTranslations.translateLegacy(
        Component.translatable("draft.gui.suggestion.lore").color(NamedTextColor.GRAY), player));
  }

  @Override
  public Material getMaterial(Player player) {
    return Material.EMERALD_BLOCK;
  }

  @Override
  public short getData() {
    return 0;
  }

  @Override
  public ItemMeta modifyMeta(ItemMeta meta) {
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    return meta;
  }

  @Override
  public void onClick(Player player, ClickType type) {
    MatchPlayer mp = PGM.get().getMatchManager().getPlayer(player);
    ctx.activatePlayerSuggestions(mp);
    player.closeInventory();
  }
}
