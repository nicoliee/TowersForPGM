package org.nicolie.towersforpgm.draft.map.gui.items;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.meta.ItemMeta;
import org.nicolie.towersforpgm.draft.map.MapVoteManager;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public final class TimerItem implements MenuItem {

  private final MapVoteManager voteManager;

  public TimerItem(MapVoteManager voteManager) {
    this.voteManager = voteManager;
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("draft.map.vote.item").color(NamedTextColor.GOLD);
  }

  @SuppressWarnings("deprecation")
  @Override
  public List<String> getLore(Player player) {
    List<Component> lore = new ArrayList<>();
    lore.add(Component.translatable(
            "misc.timeRemaining",
            Component.text(voteManager.getTimeLeft()).color(NamedTextColor.GREEN))
        .color(NamedTextColor.AQUA));
    return Lists.transform(lore, c -> TextTranslations.translateLegacy(c, player));
  }

  @Override
  public Material getMaterial(Player player) {
    return Material.WATCH;
  }

  @Override
  public short getData() {
    return 0;
  }

  @Override
  public ItemMeta modifyMeta(ItemMeta meta) {
    return meta;
  }

  @Override
  public void onClick(Player player, ClickType type) {
    // decorative — no action
  }
}
