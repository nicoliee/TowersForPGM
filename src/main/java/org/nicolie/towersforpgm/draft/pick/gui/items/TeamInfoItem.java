package org.nicolie.towersforpgm.draft.pick.gui.items;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.nicolie.towersforpgm.draft.pick.gui.SortOrder;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.util.bukkit.Sounds;
import tc.oc.pgm.util.text.TextTranslations;

public class TeamInfoItem implements MenuItem {

  private final SortOrder currentSort;
  private final DraftContext ctx;

  public TeamInfoItem(SortOrder currentSort, DraftContext ctx) {
    this.currentSort = currentSort;
    this.ctx = ctx;
  }

  @Override
  public Component getDisplayName() {
    int count = ctx.availablePlayers().getAllAvailablePlayers().size();
    String key = count != 1 ? "draft.gui.availablePlayers" : "draft.gui.availablePlayer";
    return Component.translatable(key, Component.text(count).color(NamedTextColor.DARK_AQUA))
        .color(NamedTextColor.AQUA)
        .decorate(TextDecoration.BOLD);
  }

  @Override
  public List<String> getLore(Player player) {
    int teamNumber = ctx.teams().getTeamNumber(player.getName());
    boolean isAvailable =
        ctx.availablePlayers().getAllAvailablePlayers().contains(player.getName());

    List<Component> lore = new ArrayList<>();

    if (teamNumber == -1 && !isAvailable) {
      lore.add(Component.translatable("draft.gui.noAvailablePlayers").color(NamedTextColor.GRAY));
    } else if (teamNumber == -1) {
      lore.add(Component.translatable("draft.gui.noTeam").color(NamedTextColor.GRAY));
    } else {
      Component orderStyled = ctx.getOrderStyled();
      if (orderStyled != null) {
        lore.add(Component.translatable("draft.gui.actualOrder", orderStyled)
            .color(NamedTextColor.WHITE));
      }
    }

    lore.add(Component.empty());
    for (SortOrder sort : SortOrder.values()) {
      boolean selected = currentSort == sort;
      Component prefix = selected
          ? Component.text(">> ").color(NamedTextColor.DARK_GRAY)
          : Component.text("   ").color(NamedTextColor.GRAY);
      Component label = sort.label().color(selected ? NamedTextColor.WHITE : NamedTextColor.GRAY);
      lore.add(prefix.append(label));
    }

    lore.add(Component.empty());
    lore.add(Component.translatable("draft.gui.clickToOrder").color(NamedTextColor.GRAY));

    return lore.stream().map(c -> translate(c, player)).collect(Collectors.toList());
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

  public ItemStack createColoredItem(Player player) {
    MatchPlayer mp = PGM.get().getMatchManager().getPlayer(player);

    ItemStack item = new ItemStack(Material.LEATHER_HELMET);
    LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
    meta.setColor(mp.getParty().getFullColor());
    meta.setDisplayName(translate(getDisplayName(), player));
    meta.setLore(getLore(player));
    item.setItemMeta(meta);
    return item;
  }

  @Override
  public void onClick(Player player, ClickType type) {
    MatchPlayer mp = PGM.get().getMatchManager().getPlayer(player);
    mp.playSound(Sounds.INVENTORY_CLICK);
    // Avanzar currentSort y redibujar vive en PicksMenu,
    // ya que SortOrder es estado del menú, no del ítem.
  }

  @SuppressWarnings("deprecation")
  private static String translate(Component component, Player player) {
    return TextTranslations.translateLegacy(component, player);
  }
}
