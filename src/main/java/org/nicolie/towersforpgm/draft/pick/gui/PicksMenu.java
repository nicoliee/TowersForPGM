package org.nicolie.towersforpgm.draft.pick.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import fr.minuskube.inv.content.SlotPos;
import java.util.*;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.draft.pick.gui.items.*;
import org.nicolie.towersforpgm.draft.state.DraftPhase;
import org.nicolie.towersforpgm.session.draft.DraftContext;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.PagedInventoryMenu;

public class PicksMenu extends PagedInventoryMenu {

  private static final int ROWS = 6;
  private static final int ITEMS_PER_PAGE = 28;

  private final TowersForPGM plugin;
  private final DraftContext ctx;
  private final int captainNumber;

  private final Set<String> expandedNames = new HashSet<>();
  private SortOrder currentSort = SortOrder.NAME;

  public PicksMenu(MatchPlayer viewer, TowersForPGM plugin, DraftContext ctx) {
    super(
        Component.translatable("draft.gui.title").color(NamedTextColor.DARK_GRAY),
        ROWS,
        viewer,
        null,
        ITEMS_PER_PAGE,
        1,
        1);
    this.plugin = plugin;
    this.ctx = ctx;
    this.captainNumber = ctx.getCaptainNumber(viewer.getBukkit().getUniqueId());
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    if (ctx.phase() == DraftPhase.ENDED) {
      player.closeInventory();
      return;
    }
    fillBorder(contents, player);
    placeTeamInfoButton(contents, player);
    placeSuggestionButton(contents, player);
    setupPageContents(player, contents);
    fillBackground(contents);
  }

  @Override
  public void update(Player player, InventoryContents contents) {
    init(player, contents);
  }

  @SuppressWarnings("deprecation")
  private void fillBackground(InventoryContents contents) {
    ItemStack bg = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 7);
    ItemMeta meta = bg.getItemMeta();
    meta.setDisplayName(" ");
    bg.setItemMeta(meta);
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < 9; col++) {
        if (contents.get(row, col).isEmpty()) {
          contents.set(row, col, ClickableItem.empty(bg));
        }
      }
    }
  }

  private void fillBorder(InventoryContents contents, Player player) {
    BorderItem borderItem =
        new BorderItem(ctx.captains(), ctx.availablePlayers(), ctx.teams(), captainNumber);
    ClickableItem pane = ClickableItem.empty(borderItem.createColoredItem(player));

    for (int col = 0; col < 9; col++) {
      contents.set(0, col, pane);
      contents.set(lastRow(), col, pane);
    }
    for (int row = 1; row < lastRow(); row++) {
      contents.set(row, 0, pane);
      contents.set(row, 8, pane);
    }
  }

  private void placeTeamInfoButton(InventoryContents contents, Player player) {
    contents.set(0, 4, ClickableItem.of(buildTeamInfoItem(player), e -> {
      currentSort = currentSort.next();
      e.getInventory().setItem(e.getSlot(), buildTeamInfoItem(player));
      new TeamInfoItem(currentSort, ctx).onClick(player, e.getClick());
    }));
  }

  private ItemStack buildTeamInfoItem(Player player) {
    return new TeamInfoItem(currentSort, ctx).createColoredItem(player);
  }

  private void placeSuggestionButton(InventoryContents contents, Player player) {
    if (ctx.getCaptainNumber(player.getUniqueId()) == -1) return;
    if (captainNumber != 1 && captainNumber != 2) return;
    if (ctx.teams().getAllTeam(captainNumber).size() <= 1) return;
    if (ctx.captains().isPlayerSuggestions()) return;

    boolean isTurn = (captainNumber == 1 && ctx.captains().isCaptain1Turn())
        || (captainNumber == 2 && !ctx.captains().isCaptain1Turn());
    if (!isTurn) return;

    SuggestionToggleItem toggleItem = new SuggestionToggleItem(ctx);
    contents.set(
        lastRow(),
        4,
        ClickableItem.of(
            toggleItem.createItem(player), e -> toggleItem.onClick(player, e.getClick())));
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

  @Override
  public ClickableItem[] getPageContents(Player viewer) {
    return getSortedPlayerNames().stream()
        .map(name -> buildSkullClickable(viewer, name))
        .toArray(ClickableItem[]::new);
  }

  private List<String> getSortedPlayerNames() {
    Set<String> names = new HashSet<>();
    ctx.availablePlayers().getAvailablePlayers().forEach(p -> names.add(p.getNameLegacy()));
    names.addAll(ctx.availablePlayers().getAvailableOfflinePlayers());

    if (!plugin.getIsDatabaseActivated() || currentSort == SortOrder.NAME) {
      return names.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
    }

    return names.stream()
        .sorted((a, b) -> {
          Stats sa = ctx.availablePlayers().getStatsForPlayer(a);
          Stats sb = ctx.availablePlayers().getStatsForPlayer(b);
          int va = sa != null ? currentSort.statValue(sa) : -1;
          int vb = sb != null ? currentSort.statValue(sb) : -1;
          int cmp = Integer.compare(vb, va); // descending
          return cmp != 0 ? cmp : String.CASE_INSENSITIVE_ORDER.compare(a, b);
        })
        .collect(Collectors.toList());
  }

  private ClickableItem buildSkullClickable(Player viewer, String name) {
    Stats stats =
        plugin.getIsDatabaseActivated() ? ctx.availablePlayers().getStatsForPlayer(name) : null;
    boolean expanded = expandedNames.contains(name);

    PlayerSkullItem skull = new PlayerSkullItem(name, stats, ctx, expanded);

    return ClickableItem.of(skull.createItem(viewer), e -> {
      PlayerSkullItem.ClickResult result = skull.handleClick(viewer, e.getClick());

      if (result == PlayerSkullItem.ClickResult.TOGGLE_EXPAND) {
        if (expanded) expandedNames.remove(name);
        else expandedNames.add(name);

        PlayerSkullItem updated =
            new PlayerSkullItem(name, stats, ctx, expandedNames.contains(name));
        e.getInventory().setItem(e.getSlot(), updated.createItem(viewer));
      }
    });
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
