package org.nicolie.towersforpgm.draft.picksMenu;

import static net.kyori.adventure.text.format.NamedTextColor.GRAY;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.draft.core.AvailablePlayers;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class PlayerSkullItem implements MenuItem {

  private final String name;
  private final Captains captains;
  private final AvailablePlayers availablePlayers;
  private final Teams teams;
  private final Stats stats;

  public PlayerSkullItem(
      String name, Stats stats, Captains captains, AvailablePlayers availablePlayers, Teams teams) {
    this.name = name;
    this.captains = captains;
    this.availablePlayers = availablePlayers;
    this.teams = teams;
    this.stats = stats;
  }

  @Override
  public Component getDisplayName() {
    return MatchManager.getPrefixedName(name);
  }

  @Override
  public List<String> getLore(Player player) {
    List<Component> lore = stats != null ? stats.getLore() : Lists.newArrayList();
    int captainNumber = captains.getCaptainTeam(player.getUniqueId());
    int teamNumber = teams.getTeamNumber(player.getName());
    boolean isCaptainTurn = (captainNumber == 1 && captains.isCaptain1Turn())
        || (captainNumber == 2 && !captains.isCaptain1Turn());
    boolean isSuggestionTeamTurn = (teamNumber == 1 && captains.isCaptain1Turn())
        || (teamNumber == 2 && !captains.isCaptain1Turn());

    if (captains.isCaptain(player.getUniqueId()) && isCaptainTurn) {
      lore.add(Component.space());
      lore.add(Component.translatable("draft.gui.clickToPick"));
    } else if (captains.isPlayerSuggestions()
        && !availablePlayers.hasAlreadySuggested(player.getName())
        && isSuggestionTeamTurn) {
      lore.add(Component.space());
      lore.add(Component.translatable("draft.gui.clickToSuggest"));
    }
    return Lists.transform(lore, c -> TextTranslations.translateLegacy(c.color(GRAY), player));
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
    SkullMeta skullMeta = (SkullMeta) meta;
    skullMeta.setOwner(name);
    return skullMeta;
  }

  @Override
  public void onClick(Player player, ClickType type) {
    // manejado externamente en PicksMenu
  }
}
