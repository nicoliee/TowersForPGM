package org.nicolie.towersforpgm.commands.history.gui.playerHistory;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import fr.minuskube.inv.content.SlotPos;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.commands.history.gui.match.MatchMenu;
import org.nicolie.towersforpgm.commands.history.gui.playerHistory.items.BackgroundItem;
import org.nicolie.towersforpgm.commands.history.gui.playerHistory.items.BorderItem;
import org.nicolie.towersforpgm.commands.history.gui.playerHistory.items.MatchItem;
import org.nicolie.towersforpgm.commands.history.gui.playerHistory.items.PlayerSkullItem;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.database.models.history.MatchHistory;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.PagedInventoryMenu;
import tc.oc.pgm.util.bukkit.Sounds;

public class HistoryMenu extends PagedInventoryMenu {

  public static final int HISTORY_LIMIT = 14;
  private static final int ROWS = 3;
  private static final int ITEMS_PER_PAGE = 7;

  private final Stats stats;
  private final List<MatchHistory> matches;
  private final String targetUsername;
  private final String table;
  private boolean skullExpanded = false;

  public HistoryMenu(
      MatchPlayer viewer,
      SmartInventory parent,
      String table,
      Stats stats,
      List<MatchHistory> matches,
      String targetUsername) {
    super(
        Component.translatable("history.gui.title").color(NamedTextColor.DARK_GRAY),
        ROWS,
        viewer,
        parent,
        ITEMS_PER_PAGE,
        1,
        1);
    viewer.playSound(Sounds.INVENTORY_CLICK);
    this.stats = stats;
    this.matches = matches;
    this.targetUsername = targetUsername;
    this.table = table;
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    fillBorder(contents);
    placePlayerInfoButton(contents, player);
    setupPageContents(player, contents);
    placeBackButton(contents);
    fillBackground(contents);
  }

  @Override
  public void update(Player player, InventoryContents contents) {
    init(player, contents);
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
  public ClickableItem[] getPageContents(Player player) {
    if (matches == null || matches.isEmpty()) {
      return new ClickableItem[0];
    }

    return matches.stream()
        .map(match -> buildMatchClickable(player, match))
        .toArray(ClickableItem[]::new);
  }

  @Override
  public SlotPos getEmptyPageSlot() {
    return SlotPos.of(1, 4);
  }

  @Override
  public SlotPos getPreviousPageSlot() {
    return SlotPos.of(0, 3);
  }

  @Override
  public SlotPos getNextPageSlot() {
    return SlotPos.of(0, 5);
  }

  private ClickableItem buildMatchClickable(Player player, MatchHistory match) {
    MatchItem matchItem = new MatchItem(match, targetUsername);
    return ClickableItem.of(matchItem.createItem(player), e -> onMatchClick(player, match));
  }

  protected void onMatchClick(Player player, MatchHistory match) {
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    if (matchPlayer == null) return;
    new MatchMenu(matchPlayer, getInventory(), match, table).open();
  }

  private void placeBackButton(InventoryContents contents) {
    if (getInventory().getParent().isEmpty()) return;

    Component parentTitle =
        Component.text(getInventory().getParent().map(SmartInventory::getTitle).orElse(""));
    addBackButton(contents, Component.translatable("menu.page.return", parentTitle), lastRow(), 4);
  }

  private void fillBorder(InventoryContents contents) {
    BorderItem borderItem = new BorderItem();
    ClickableItem pane = ClickableItem.empty(borderItem.createItem(null));

    for (int col = 0; col < 9; col++) {
      contents.set(0, col, pane);
      contents.set(lastRow(), col, pane);
    }
    for (int row = 1; row < lastRow(); row++) {
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
    PlayerSkullItem skull = new PlayerSkullItem(targetUsername, table, stats, skullExpanded);
    contents.set(0, 4, ClickableItem.of(skull.createItem(player), e -> {
      skullExpanded = !skullExpanded;
      PlayerSkullItem updated = new PlayerSkullItem(targetUsername, table, stats, skullExpanded);
      e.getInventory().setItem(e.getSlot(), updated.createItem(player));
    }));
  }

  private void fillBackground(InventoryContents contents) {
    BackgroundItem bg = new BackgroundItem();
    ClickableItem pane = ClickableItem.empty(bg.createItem(null));

    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < 9; col++) {
        if (contents.get(row, col).isEmpty()) {
          contents.set(row, col, pane);
        }
      }
    }
  }
}
