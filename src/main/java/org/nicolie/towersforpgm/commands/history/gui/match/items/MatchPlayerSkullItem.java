package org.nicolie.towersforpgm.commands.history.gui.match.items;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.nicolie.towersforpgm.database.models.history.MatchHistory;
import org.nicolie.towersforpgm.database.models.history.MatchStats;
import org.nicolie.towersforpgm.database.models.history.PlayerHistory;
import org.nicolie.towersforpgm.rankeds.Rank;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class MatchPlayerSkullItem implements MenuItem {

  private final MatchHistory matchHistory;
  private final PlayerHistory playerHistory;

  public MatchPlayerSkullItem(MatchHistory matchHistory, PlayerHistory playerHistory) {
    this.matchHistory = matchHistory;
    this.playerHistory = playerHistory;
  }

  @Override
  public Component getDisplayName() {
    Rank rank = Rank.getRankByElo(playerHistory.getEloAfter());
    boolean isRanked = matchHistory.isRanked();
    boolean positive = playerHistory.getEloDelta() > 0;
    String eloDeltaStr = (positive ? "+" : "") + playerHistory.getEloDelta();
    Component name = MatchManager.getPrefixedName(playerHistory.getUsername());
    Component eloName = Component.text(rank.getPrefixedRank(true))
        .append(Component.space())
        .append(name)
        .append(Component.space())
        .append(Component.text(playerHistory.getEloAfter()).color(NamedTextColor.WHITE))
        .append(Component.text(" ("))
        .color(NamedTextColor.DARK_GRAY)
        .append(
            Component.text(eloDeltaStr).color(positive ? NamedTextColor.GREEN : NamedTextColor.RED))
        .append(Component.text(")"))
        .color(NamedTextColor.DARK_GRAY);
    return isRanked ? eloName : name;
  }

  @Override
  @SuppressWarnings("deprecation")
  public List<String> getLore(Player player) {
    List<Component> lore = new ArrayList<>();

    MatchStats ms = playerHistory.getMatchStats();
    lore.addAll(ms.getComponent());
    lore.add(Component.translatable(
            "history.gui.click.history", MatchManager.getPrefixedName(playerHistory.getUsername()))
        .color(NamedTextColor.GRAY));
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
    ((SkullMeta) meta).setOwner(playerHistory.getUsername());
    return meta;
  }

  @Override
  public void onClick(Player player, ClickType type) {}

  public PlayerHistory getPlayerHistory() {
    return playerHistory;
  }
}
