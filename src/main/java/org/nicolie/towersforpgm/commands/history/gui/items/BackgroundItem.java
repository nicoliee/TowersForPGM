package org.nicolie.towersforpgm.commands.history.gui.items;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.menu.MenuItem;

public class BackgroundItem implements MenuItem {

  @Override
  public Component getDisplayName() {
    return Component.space();
  }

  @Override
  public List<String> getLore(Player player) {
    return List.of();
  }

  @Override
  public Material getMaterial(Player player) {
    return Material.STAINED_GLASS_PANE;
  }

  @Override
  public short getData() {
    return 7;
  }

  @Override
  public ItemMeta modifyMeta(ItemMeta meta) {
    return meta;
  }

  @Override
  public void onClick(Player player, ClickType type) {
    // Decorativo, sin acción
  }
}
