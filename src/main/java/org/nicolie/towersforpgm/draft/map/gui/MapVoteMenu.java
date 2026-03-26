package org.nicolie.towersforpgm.draft.map.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import fr.minuskube.inv.content.SlotPos;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.draft.map.MapVoteConfig;
import org.nicolie.towersforpgm.draft.map.MapVoteManager;
import org.nicolie.towersforpgm.draft.map.gui.items.MapItem;
import org.nicolie.towersforpgm.draft.map.gui.items.TimerItem;
import org.nicolie.towersforpgm.draft.pick.gui.items.BackgroundItem;
import org.nicolie.towersforpgm.draft.pick.gui.items.BorderItem;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.PagedInventoryMenu;

public final class MapVoteMenu extends PagedInventoryMenu {

  private static final int ROWS_PLURALITY = 3;
  private static final int ROWS_VETO = 4;
  private static final int ITEMS_PER_PAGE_VETO = 14; // 7 cols × 2 content rows
  private static final int[] COLS_2 = {3, 5};
  private static final int[] COLS_3 = {2, 4, 6};

  private final MapVoteManager voteManager;
  private final int rows;

  public MapVoteMenu(MatchPlayer viewer, MapVoteManager voteManager) {
    super(
        Component.translatable(titleKeyForVoteMode(voteManager.getConfig().getVoteMode()))
            .color(NamedTextColor.DARK_GRAY),
        rowsForVoteMode(voteManager.getConfig().getVoteMode()),
        viewer,
        null,
        itemsPerPageForVoteMode(voteManager.getConfig().getVoteMode()),
        1,
        1);
    this.voteManager = voteManager;
    this.rows = rowsForVoteMode(voteManager.getConfig().getVoteMode());
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    fillBorder(contents);
    fillBackground(contents, player);
    placeTimerItem(contents, player);
    switch (voteManager.getConfig().getVoteMode()) {
      case VETO:
        setupPageContents(player, contents);
        break;
      case PLURALITY:
      default:
        placePluralityMaps(contents, player);
        break;
    }
  }

  private static String titleKeyForVoteMode(MapVoteConfig.VoteMode voteMode) {
    switch (voteMode) {
      case VETO:
        return "draft.map.veto.title";
      case PLURALITY:
      default:
        return "draft.map.title";
    }
  }

  private static int rowsForVoteMode(MapVoteConfig.VoteMode voteMode) {
    switch (voteMode) {
      case VETO:
        return ROWS_VETO;
      case PLURALITY:
      default:
        return ROWS_PLURALITY;
    }
  }

  private static int itemsPerPageForVoteMode(MapVoteConfig.VoteMode voteMode) {
    switch (voteMode) {
      case VETO:
        return ITEMS_PER_PAGE_VETO;
      case PLURALITY:
      default:
        return ROWS_PLURALITY;
    }
  }

  @Override
  public void update(Player player, InventoryContents contents) {
    init(player, contents);
  }

  @Override
  public void setupPageContents(Player player, InventoryContents contents) {
    if (voteManager.getConfig().getVoteMode() != MapVoteConfig.VoteMode.VETO) return;

    ClickableItem[] items = getPageContents(player);
    if (items == null || items.length == 0) return;

    Pagination page = contents.pagination();
    page.setItems(items);
    page.setItemsPerPage(ITEMS_PER_PAGE_VETO);

    SlotIterator iterator = contents.newIterator(SlotIterator.Type.HORIZONTAL, 1, 1);
    for (int row = 0; row < rows; row++) {
      iterator.blacklist(SlotPos.of(row, 0));
      iterator.blacklist(SlotPos.of(row, 8));
    }
    for (int col = 0; col < 9; col++) {
      iterator.blacklist(SlotPos.of(0, col));
      iterator.blacklist(SlotPos.of(lastRow(), col));
    }
    page.addToIterator(iterator);

    if (!page.isFirst()) {
      contents.set(
          getPreviousPageSlot(), getPageItem(player, page.getPage() - 1, "menu.page.previous"));
    }
    if (!page.isLast()) {
      contents.set(getNextPageSlot(), getPageItem(player, page.getPage() + 1, "menu.page.next"));
    }
  }

  private void placeTimerItem(InventoryContents contents, Player player) {
    TimerItem timerItem = new TimerItem(voteManager);
    contents.set(0, 4, ClickableItem.empty(timerItem.createItem(player)));
  }

  @Override
  public ClickableItem[] getPageContents(Player viewer) {
    if (voteManager.getConfig().getVoteMode() != MapVoteConfig.VoteMode.VETO)
      return new ClickableItem[0];

    List<String> maps = voteManager.getConfig().getMaps();
    ClickableItem[] items = new ClickableItem[maps.size()];
    for (int i = 0; i < maps.size(); i++) {
      final String name = maps.get(i);
      final int idx = i;
      boolean isSelected = name.equals(voteManager.getCurrentVote(viewer.getUniqueId()));
      MapItem item = new MapItem(name, idx, voteManager, isSelected);
      items[i] = createMapClickable(viewer, item);
    }
    return items;
  }

  private void placePluralityMaps(InventoryContents contents, Player player) {
    List<String> displayed = voteManager.getDisplayedMaps();
    int count = displayed.size(); // 2 or 3

    int[] cols = count == 2 ? COLS_2 : COLS_3;

    for (int i = 0; i < count; i++) {
      final String name = displayed.get(i);
      final int col = cols[i];
      final int idx = i;
      boolean isSelected = name.equals(voteManager.getCurrentVote(player.getUniqueId()));
      MapItem item = new MapItem(name, idx, voteManager, isSelected);
      contents.set(1, col, createMapClickable(player, item));
    }
  }

  private ClickableItem createMapClickable(Player player, MapItem item) {
    return ClickableItem.of(item.createItem(player), e -> {
      item.onClick(player, e.getClick());
      open();
    });
  }

  private void fillBorder(InventoryContents contents) {
    ClickableItem border = ClickableItem.empty(BorderItem.blackPane());
    for (int col = 0; col < 9; col++) {
      contents.set(0, col, border);
      contents.set(lastRow(), col, border);
    }
    for (int row = 1; row < lastRow(); row++) {
      contents.set(row, 0, border);
      contents.set(row, 8, border);
    }
  }

  private void fillBackground(InventoryContents contents, Player player) {
    BackgroundItem bgItem = new BackgroundItem();
    ClickableItem bg = ClickableItem.empty(bgItem.createItem(player));
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < 9; col++) {
        if (contents.get(row, col).isEmpty()) {
          contents.set(row, col, bg);
        }
      }
    }
  }

  @Override
  public SlotPos getEmptyPageSlot() {
    return SlotPos.of(0, 4);
  }

  @Override
  public SlotPos getPreviousPageSlot() {
    return SlotPos.of(0, 3);
  }

  @Override
  public SlotPos getNextPageSlot() {
    return SlotPos.of(0, 5);
  }
}
