package org.nicolie.towersforpgm.commands.history.gui.items;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class PlayerSkullItem implements MenuItem {
  public enum ClickResult {
    TOGGLE_EXPAND,
    ACTION_TAKEN,
    NONE
  }

  private final String targetName;
  private final Stats stats;
  private final boolean expanded;

  public PlayerSkullItem(String targetName, Stats stats, boolean expanded) {
    this.targetName = targetName;
    this.stats = stats;
    this.expanded = expanded;
  }

  @Override
  public Component getDisplayName() {
    return MatchManager.getPrefixedName(targetName);
  }

  @SuppressWarnings("deprecation")
  @Override
  public List<String> getLore(Player player) {
    List<Component> lore = (stats != null && expanded)
        ? stats.getDetailedLore()
        : stats != null ? stats.getLore() : new java.util.ArrayList<>();

    if (stats != null) {
      lore.add(Component.space());
      lore.add(Component.translatable(
              expanded ? "draft.gui.rightClick.back" : "draft.gui.rightClick.details")
          .color(NamedTextColor.GRAY));
    }

    return Lists.transform(lore, c -> TextTranslations.translateLegacy(c, player));
  }

  @Override
  public Material getMaterial(Player player) {
    return Material.SKULL_ITEM;
  }

  @Override
  public void onClick(Player player, ClickType type) {
    /* Al hacer click en el item, se alterna entre mostrar detalles o no
    La lógica de esto se maneja en el menú que contiene este item */
  }
}
