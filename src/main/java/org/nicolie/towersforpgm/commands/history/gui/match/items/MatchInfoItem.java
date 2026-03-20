package org.nicolie.towersforpgm.commands.history.gui.match.items;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.nicolie.towersforpgm.database.models.history.MatchHistory;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class MatchInfoItem implements MenuItem {
  private final MatchHistory matchHistory;

  public MatchInfoItem(MatchHistory matchHistory) {
    this.matchHistory = matchHistory;
  }

  @Override
  public Component getDisplayName() {
    return Component.text(matchHistory.getMapName())
        .color(NamedTextColor.GRAY)
        .decorate(TextDecoration.BOLD);
  }

  @SuppressWarnings("deprecation")
  @Override
  public List<String> getLore(Player player) {
    List<Component> lore = new ArrayList<>();
    matchHistory.getFormattedInfo().forEach(lore::add);
    return Lists.transform(lore, c -> TextTranslations.translateLegacy(c, player));
  }

  @Override
  public Material getMaterial(Player arg0) {
    return Material.PAPER;
  }

  @Override
  public void onClick(Player arg0, ClickType arg1) {
    // No hace nada al hacer click
  }
}
