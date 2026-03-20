package org.nicolie.towersforpgm.commands.history.gui.match.items;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.nicolie.towersforpgm.commands.history.gui.match.MatchSortOrder;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.util.bukkit.Sounds;
import tc.oc.pgm.util.text.TextTranslations;

public class TeamHeaderItem implements MenuItem {

  private final String teamName;
  private final String teamColorHex;
  private final int playerCount;
  private final MatchSortOrder currentSort;

  public TeamHeaderItem(
      String teamName, String teamColorHex, int playerCount, MatchSortOrder currentSort) {
    this.teamName = teamName;
    this.teamColorHex = teamColorHex;
    this.playerCount = playerCount;
    this.currentSort = currentSort;
  }

  @Override
  public Component getDisplayName() {
    TextColor color = resolveColor(teamColorHex);
    return Component.text(teamName)
        .color(color)
        .decoration(TextDecoration.ITALIC, false)
        .decoration(TextDecoration.BOLD, true);
  }

  @Override
  @SuppressWarnings("deprecation")
  public List<String> getLore(Player player) {
    List<Component> lore = new ArrayList<>();

    lore.add(Component.text(playerCount + " jugadores")
        .color(NamedTextColor.GRAY)
        .decoration(TextDecoration.ITALIC, false));

    lore.add(Component.empty());

    for (MatchSortOrder sort : MatchSortOrder.values()) {
      boolean selected = currentSort == sort;
      Component prefix = selected
          ? Component.text(">> ").color(NamedTextColor.DARK_GRAY)
          : Component.text("   ").color(NamedTextColor.GRAY);
      lore.add(prefix.append(sort.label()
          .color(selected ? NamedTextColor.WHITE : NamedTextColor.GRAY)
          .decoration(TextDecoration.ITALIC, false)));
    }

    lore.add(Component.empty());
    lore.add(Component.translatable("history.gui.click.sort").color(NamedTextColor.DARK_GRAY));

    return Lists.transform(lore, c -> TextTranslations.translateLegacy(c, player));
  }

  @Override
  public Material getMaterial(Player player) {
    return Material.LEATHER_HELMET;
  }

  @Override
  public short getData() {
    return 0;
  }

  @Override
  public ItemMeta modifyMeta(ItemMeta meta) {
    return meta;
  }

  @SuppressWarnings("deprecation")
  public ItemStack createColoredItem(Player player) {
    TextColor color = resolveColor(teamColorHex);
    int rgb = color.value();
    org.bukkit.Color bukkitColor =
        org.bukkit.Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);

    ItemStack item = new ItemStack(Material.LEATHER_HELMET);
    LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
    meta.setColor(bukkitColor);
    meta.setDisplayName(TextTranslations.translateLegacy(getDisplayName(), player));
    meta.setLore(getLore(player));
    item.setItemMeta(meta);
    return item;
  }

  @Override
  public void onClick(Player player, ClickType type) {
    MatchPlayer viewer = PGM.get().getMatchManager().getPlayer(player);
    viewer.playSound(Sounds.INVENTORY_CLICK);
  }

  private TextColor resolveColor(String hex) {
    if (hex != null && !hex.isBlank()) {
      TextColor parsed = TextColor.fromHexString(hex);
      if (parsed != null) return parsed;
    }
    return NamedTextColor.GRAY;
  }
}
