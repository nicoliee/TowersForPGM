package org.nicolie.towersforpgm.commands.history.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import fr.minuskube.inv.content.SlotPos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.commands.history.gui.items.BackgroundItem;
import org.nicolie.towersforpgm.commands.history.gui.items.BorderItem;
import org.nicolie.towersforpgm.commands.history.gui.items.PlayerSkullItem;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.database.models.history.MatchHistory;
import tc.oc.pgm.menu.PagedInventoryMenu;

public class HistoryMenu extends PagedInventoryMenu {

  private static final int ROWS = 3;
  private static final int ITEMS_PER_PAGE = 7;

  private final Stats stats;
  private final MatchHistory matchHistory;

  public HistoryMenu(Stats stats, MatchHistory matchHistory) {
    super(
        Component.translatable("history.gui.title").color(NamedTextColor.DARK_GRAY),
        ROWS,
        null,
        null,
        ITEMS_PER_PAGE,
        1,
        1);
    this.stats = stats;
    this.matchHistory = matchHistory;
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    fillBorder(contents, player);
    placePlayerInfoButton(contents, player);
    setupPageContents(contents, player);
    fillBackground(contents, player);
  }

  @Override
  public void setupPageContents(Player player, InventoryContents contents) {
    ClickableItem[] items = getPageContents(player);
    if (items == null || items.length == 0) {
      contents.set(getEmptyPageSlot(), getEmptyContentsButton(player));
      return;
    }

    Pagination page = contents.pagination();
    page.setItems(items);
    page.setItemsPerPage(ITEMS_PER_PAGE);

    SlotIterator iterator = contents.newIterator(SlotIterator.Type.HORIZONTAL, 1, 1);
    blacklistBorderSlots(iterator);
    page.addToIterator(iterator);

    if (!page.isFirst()) {
      contents.set(
          getPreviousPageSlot(), getPageItem(player, page.getPage() - 1, "menu.page.previous"));
    }
    if (!page.isLast()) {
      contents.set(getNextPageSlot(), getPageItem(player, page.getPage() + 1, "menu.page.next"));
    }
  }

  @Override
  public ClickableItem[] getPageContents(Player arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SlotPos getEmptyPageSlot() {
    return SlotPos.of(1, 4); // Centro del inventario
  }

  @Override
  public SlotPos getPreviousPageSlot() {
    return SlotPos.of(0, 3);
  }

  @Override
  public SlotPos getNextPageSlot() {
    return SlotPos.of(0, 5);
  }

  private void fillBorder(InventoryContents contents, Player player) {
    BorderItem borderItem = new BorderItem();
    ClickableItem pane = ClickableItem.empty(borderItem.createItem(player));

    for (int col = 0; col < 9; col++) {
      contents.set(0, col, pane);
      contents.set(ROWS - 1, col, pane);
    }

    for (int row = 1; row < ROWS - 1; row++) {
      contents.set(row, 0, pane);
      contents.set(row, 8, pane);
    }
  }

  private void blacklistBorderSlots(SlotIterator iterator) {
    for (int col = 0; col < 9; col++) {
      iterator.blacklist(SlotPos.of(0, col));
      iterator.blacklist(SlotPos.of(lastRow(), col));
    }
    for (int row = 1; row < lastRow(); row++) {
      iterator.blacklist(SlotPos.of(row, 0));
      iterator.blacklist(SlotPos.of(row, 8));
    }
  }

  private void placePlayerInfoButton(InventoryContents contents, Player player) {
    PlayerSkullItem playerInfo = new PlayerSkullItem(player.getName(), stats, false);
    contents.set(0, 4, ClickableItem.empty(playerInfo.createItem(player)));
  }

  private void setupPageContents(InventoryContents contents, Player player) {}

  private void fillBackground(InventoryContents contents, Player player) {
    BackgroundItem bg = new BackgroundItem();
    ClickableItem pane = ClickableItem.empty(bg.createItem(player));

    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < 9; col++) {
        if (contents.get(row, col).isEmpty()) {
          contents.set(row, col, pane);
        }
      }
    }
  }
}
