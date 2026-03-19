package org.nicolie.towersforpgm.commands.history.gui.items;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.menu.MenuItem;

public class BorderItem implements MenuItem {

  @Override
  public Component getDisplayName() {
    return Component.space();
  }

  @Override
  public List<String> getLore(Player arg0) {
    return List.of();
  }

  @Override
  public Material getMaterial(Player arg0) {
    return Material.STAINED_GLASS_PANE;
  }

  @Override
  public short getData() {
    return 15;
  }

  @Override
  public void onClick(Player arg0, ClickType arg1) {
    // Decorativo, sin acción

  }
}
