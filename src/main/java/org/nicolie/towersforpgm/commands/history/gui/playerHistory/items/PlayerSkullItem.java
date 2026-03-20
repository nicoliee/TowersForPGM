package org.nicolie.towersforpgm.commands.history.gui.playerHistory.items;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class PlayerSkullItem implements MenuItem {

  private final String targetName;
  private final String table;
  private final Stats stats;
  private final boolean expanded;

  public PlayerSkullItem(String targetName, String table, Stats stats, boolean expanded) {
    this.targetName = targetName;
    this.table = table;
    this.stats = stats;
    this.expanded = expanded;
  }

  @Override
  public Component getDisplayName() {
    Component base = Component.text()
        .append(MatchManager.getPrefixedName(targetName))
        .append(Component.text(" - ").color(NamedTextColor.DARK_GRAY))
        .append(Component.text(table).color(NamedTextColor.GRAY))
        .build();
    return base;
  }

  @SuppressWarnings("deprecation")
  @Override
  public List<String> getLore(Player player) {
    List<Component> lore = (stats != null && expanded)
        ? stats.getDetailedLore()
        : stats != null ? stats.getLore() : new java.util.ArrayList<>();

    if (stats != null) {
      lore.add(Component.space());
      lore.add(
          Component.translatable(expanded ? "history.gui.click.back" : "history.gui.click.next")
              .color(NamedTextColor.GRAY));
    }

    return Lists.transform(lore, c -> TextTranslations.translateLegacy(c, player));
  }

  @Override
  public Material getMaterial(Player player) {
    return Material.SKULL_ITEM;
  }

  @Override
  public short getData() {
    return 3;
  }

  @Override
  public ItemMeta modifyMeta(ItemMeta meta) {
    ((SkullMeta) meta).setOwner(targetName);
    return meta;
  }

  @Override
  public void onClick(Player player, ClickType type) {
    /* Al hacer click en el item, se alterna entre mostrar detalles o no
    La lógica de esto se maneja en el menú que contiene este item */
  }
}
