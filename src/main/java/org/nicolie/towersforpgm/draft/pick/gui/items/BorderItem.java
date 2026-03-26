package org.nicolie.towersforpgm.draft.pick.gui.items;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nicolie.towersforpgm.draft.team.AvailablePlayers;
import org.nicolie.towersforpgm.draft.team.Captains;
import org.nicolie.towersforpgm.draft.team.Teams;
import tc.oc.pgm.menu.MenuItem;

public class BorderItem implements MenuItem {

  private final Captains captains;
  private final AvailablePlayers availablePlayers;
  private final Teams teams;
  private final int captainNumber;

  public BorderItem(
      Captains captains, AvailablePlayers availablePlayers, Teams teams, int captainNumber) {
    this.captains = captains;
    this.availablePlayers = availablePlayers;
    this.teams = teams;
    this.captainNumber = captainNumber;
  }

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
    return 15;
  }

  public byte resolveBorderColor(Player player) {
    boolean isCaptain = captains.isCaptain(player.getUniqueId());
    boolean isCaptainTurn = (captainNumber == 1 && captains.isCaptain1Turn())
        || (captainNumber == 2 && !captains.isCaptain1Turn());

    if (isCaptain) {
      return isCaptainTurn ? (byte) 5 : (byte) 14;
    }

    int teamNumber = teams.getTeamNumber(player.getName());
    boolean canSuggest = captains.isPlayerSuggestions()
        && !availablePlayers.hasAlreadySuggested(player.getName())
        && ((teamNumber == 1 && captains.isCaptain1Turn())
            || (teamNumber == 2 && !captains.isCaptain1Turn()));

    return canSuggest ? (byte) 5 : (byte) 15;
  }

  @Override
  public ItemMeta modifyMeta(ItemMeta meta) {
    return meta;
  }

  @Override
  public void onClick(Player player, ClickType type) {
    // decorativo, sin acción
  }

  public ItemStack createColoredItem(Player player) {
    byte color = resolveBorderColor(player);
    ItemStack pane = new ItemStack(getMaterial(player), 1, color);
    ItemMeta meta = pane.getItemMeta();
    meta.setDisplayName(" ");
    pane.setItemMeta(meta);
    return pane;
  }

  public static ItemStack blackPane() {
    ItemStack pane = new ItemStack(org.bukkit.Material.STAINED_GLASS_PANE, 1, (short) 15);
    ItemMeta meta = pane.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(" ");
      pane.setItemMeta(meta);
    }
    return pane;
  }
}
