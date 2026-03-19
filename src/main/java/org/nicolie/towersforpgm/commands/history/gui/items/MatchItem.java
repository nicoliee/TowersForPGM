package org.nicolie.towersforpgm.commands.history.gui.items;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.nicolie.towersforpgm.database.models.history.MatchHistory;
import tc.oc.pgm.menu.MenuItem;

public class MatchItem implements MenuItem {

  private final MatchHistory matchHistory;

  public MatchItem(MatchHistory matchHistory) {
    this.matchHistory = matchHistory;
  }

  @Override
  public Component getDisplayName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<String> getLore(Player arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Material getMaterial(Player arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void onClick(Player arg0, ClickType arg1) {
    // TODO Auto-generated method stub

  }

  @Override
  public short getData() {
    // TODO Auto-generated method stub
    return MenuItem.super.getData();
  }
}
