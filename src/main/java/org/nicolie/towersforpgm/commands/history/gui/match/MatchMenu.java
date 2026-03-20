package org.nicolie.towersforpgm.commands.history.gui.match;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.SlotPos;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.commands.history.gui.match.items.MatchInfoItem;
import org.nicolie.towersforpgm.commands.history.gui.match.items.MatchPlayerSkullItem;
import org.nicolie.towersforpgm.commands.history.gui.match.items.TeamHeaderItem;
import org.nicolie.towersforpgm.commands.history.gui.playerHistory.HistoryMenu;
import org.nicolie.towersforpgm.commands.history.gui.playerHistory.items.BackgroundItem;
import org.nicolie.towersforpgm.commands.history.gui.playerHistory.items.BorderItem;
import org.nicolie.towersforpgm.database.MatchHistoryManager;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.database.models.history.MatchHistory;
import org.nicolie.towersforpgm.database.models.history.PlayerHistory;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.PagedInventoryMenu;
import tc.oc.pgm.util.bukkit.Sounds;
import tc.oc.pgm.util.text.TextTranslations;

public class MatchMenu extends PagedInventoryMenu {

  private static final int ROWS = 6;
  private static final int ITEMS_PER_TEAM_PAGE = 16;
  private static final int[] LEFT_COLS = {0, 1, 2, 3};
  private static final int[] RIGHT_COLS = {5, 6, 7, 8};

  private final MatchHistory matchHistory;
  private final String table;

  private int leftPage = 0;
  private int rightPage = 0;
  private MatchSortOrder leftSort = MatchSortOrder.NAME;
  private MatchSortOrder rightSort = MatchSortOrder.NAME;

  private final List<String> teamNames;
  private final Map<String, List<PlayerHistory>> teamPlayers;
  private final Map<String, String> teamColors;

  public MatchMenu(
      MatchPlayer viewer, SmartInventory parent, MatchHistory matchHistory, String table) {
    super(
        Component.translatable("history.gui.match").color(NamedTextColor.DARK_GRAY),
        ROWS,
        viewer,
        parent,
        0,
        1,
        1);
    viewer.playSound(Sounds.INVENTORY_CLICK);
    this.matchHistory = matchHistory;
    this.table = table;
    this.teamNames = new ArrayList<>();
    this.teamPlayers = new LinkedHashMap<>();
    this.teamColors = new LinkedHashMap<>();
    resolveTeams();
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    fillBorder(contents, player);
    placeSeparator(contents, player);
    placeTeamHeaders(contents, player);
    placeMatchHeader(contents, player);
    placeTeamPlayers(contents, player);
    placeNavigation(contents, player);
    placeBackButton(contents);
    fillBackground(contents, player);
  }

  @Override
  public void update(Player player, InventoryContents contents) {
    init(player, contents);
  }

  @Override
  public void setupPageContents(Player player, InventoryContents contents) {}

  @Override
  public ClickableItem[] getPageContents(Player player) {
    return new ClickableItem[0];
  }

  @Override
  public SlotPos getEmptyPageSlot() {
    return SlotPos.of(2, 4);
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
      contents.set(lastRow(), col, pane);
    }
  }

  private void placeSeparator(InventoryContents contents, Player player) {
    ClickableItem sep = ClickableItem.empty(new BorderItem().createItem(player));
    for (int row = 0; row < ROWS; row++) {
      contents.set(row, 4, sep);
    }
  }

  private void fillBackground(InventoryContents contents, Player player) {
    ClickableItem bg = ClickableItem.empty(new BackgroundItem().createItem(player));
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < 9; col++) {
        if (contents.get(row, col).isEmpty()) {
          contents.set(row, col, bg);
        }
      }
    }
  }

  private void placeTeamHeaders(InventoryContents contents, Player player) {
    placeTeamHeader(contents, player, 0, 1, leftSort, forward -> {
      leftSort = forward ? leftSort.next() : leftSort.previous();
      leftPage = 0;
      update(player, contents);
    });
    placeTeamHeader(contents, player, 1, 7, rightSort, forward -> {
      rightSort = forward ? rightSort.next() : rightSort.previous();
      rightPage = 0;
      update(player, contents);
    });
  }

  private void placeMatchHeader(InventoryContents contents, Player player) {
    contents.set(0, 4, ClickableItem.empty(new MatchInfoItem(matchHistory).createItem(player)));
  }

  private void placeTeamHeader(
      InventoryContents contents,
      Player player,
      int teamIdx,
      int col,
      MatchSortOrder sort,
      java.util.function.Consumer<Boolean> onClick) {
    if (teamNames.size() <= teamIdx) return;
    String name = teamNames.get(teamIdx);
    String color = teamColors.getOrDefault(name, "#AAAAAA");
    int count = teamPlayers.getOrDefault(name, List.of()).size();
    TeamHeaderItem header = new TeamHeaderItem(name, color, count, sort);
    contents.set(0, col, ClickableItem.of(header.createColoredItem(player), e -> {
      MatchPlayer mp = PGM.get().getMatchManager().getPlayer(player);
      if (mp != null) mp.playSound(Sounds.INVENTORY_CLICK);
      boolean forward = !e.isRightClick();
      onClick.accept(forward);
    }));
  }

  private void placeTeamPlayers(InventoryContents contents, Player player) {
    if (teamNames.size() >= 1)
      placeTeamPage(contents, player, teamNames.get(0), LEFT_COLS, leftPage, leftSort);
    if (teamNames.size() >= 2)
      placeTeamPage(contents, player, teamNames.get(1), RIGHT_COLS, rightPage, rightSort);
  }

  private void placeTeamPage(
      InventoryContents contents,
      Player player,
      String teamName,
      int[] cols,
      int page,
      MatchSortOrder sort) {
    List<PlayerHistory> sorted = getSortedPlayers(teamName, sort);
    int from = page * ITEMS_PER_TEAM_PAGE;
    int to = Math.min(from + ITEMS_PER_TEAM_PAGE, sorted.size());
    int idx = 0;

    outer:
    for (int row = 1; row <= 4; row++) {
      for (int col : cols) {
        if (from + idx >= to) break outer;
        PlayerHistory ph = sorted.get(from + idx);
        contents.set(
            row,
            col,
            ClickableItem.of(
                new MatchPlayerSkullItem(matchHistory, ph).createItem(player),
                e -> onSkullClick(player, ph)));
        idx++;
      }
    }
  }

  private void placeNavigation(InventoryContents contents, Player player) {
    placeTeamNav(contents, player, 0, 0, 3, () -> leftPage--, () -> leftPage++);
    placeTeamNav(contents, player, 1, 5, 8, () -> rightPage--, () -> rightPage++);
  }

  @SuppressWarnings("deprecation")
  private void placeTeamNav(
      InventoryContents contents,
      Player player,
      int teamIdx,
      int prevCol,
      int nextCol,
      Runnable dec,
      Runnable inc) {
    if (teamNames.size() <= teamIdx) return;
    List<PlayerHistory> members = teamPlayers.getOrDefault(teamNames.get(teamIdx), List.of());
    int maxPage = Math.max(0, (members.size() - 1) / ITEMS_PER_TEAM_PAGE);
    int page = teamIdx == 0 ? leftPage : rightPage;

    if (page > 0)
      contents.set(
          lastRow(),
          prevCol,
          ClickableItem.of(
              navItem(TextTranslations.translateLegacy(
                  Component.translatable("menu.page.previous"), player)),
              e -> {
                dec.run();
                update(player, contents);
              }));
    if (page < maxPage)
      contents.set(
          lastRow(),
          nextCol,
          ClickableItem.of(
              navItem(TextTranslations.translateLegacy(
                  Component.translatable("menu.page.next"), player)),
              e -> {
                inc.run();
                update(player, contents);
              }));
  }

  private void placeBackButton(InventoryContents contents) {
    if (getInventory().getParent().isEmpty()) return;

    Component parentTitle =
        Component.text(getInventory().getParent().map(SmartInventory::getTitle).orElse(""));
    addBackButton(contents, Component.translatable("menu.page.return", parentTitle), lastRow(), 4);
  }

  private void onSkullClick(Player player, PlayerHistory ph) {
    MatchPlayer mp = PGM.get().getMatchManager().getPlayer(player);
    if (mp == null) return;
    StatsManager.getStats(table, ph.getUsername())
        .thenCombine(
            MatchHistoryManager.getPlayerMatchHistory(
                ph.getUsername(), table, HistoryMenu.HISTORY_LIMIT),
            (stats, matches) -> {
              Bukkit.getScheduler().runTask(TowersForPGM.getInstance(), () -> new HistoryMenu(
                      mp, getInventory(), table, stats, matches, ph.getUsername())
                  .open());
              return null;
            });
  }

  private void resolveTeams() {
    if (matchHistory.getPlayers() == null) return;
    for (PlayerHistory ph : matchHistory.getPlayers()) {
      String tName = ph.getTeamName();
      if (tName == null || tName.isBlank()) continue;
      teamPlayers.computeIfAbsent(tName, k -> new ArrayList<>()).add(ph);
      teamColors.putIfAbsent(tName, ph.getTeamColorHex());
    }
    teamNames.addAll(teamPlayers.keySet());
  }

  private List<PlayerHistory> getSortedPlayers(String teamName, MatchSortOrder sort) {
    List<PlayerHistory> base = new ArrayList<>(teamPlayers.getOrDefault(teamName, List.of()));

    if (sort == MatchSortOrder.NAME) {
      base.sort(
          Comparator.comparing((PlayerHistory ph) -> "Unknown".equalsIgnoreCase(ph.getUsername()))
              .thenComparing(
                  ph -> ph.getUsername() == null ? "" : ph.getUsername(),
                  String.CASE_INSENSITIVE_ORDER));
      return base;
    }

    base.sort(Comparator.comparingDouble(ph -> -sort.getValue(ph)));
    return base;
  }

  private ItemStack navItem(String label) {
    ItemStack item = new ItemStack(Material.ARROW);
    ItemMeta meta = item.getItemMeta();
    meta.setDisplayName("§7" + label);
    item.setItemMeta(meta);
    return item;
  }
}
