package org.nicolie.towersforpgm.draft.picksMenu;

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
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.draft.components.DraftPhase;
import org.nicolie.towersforpgm.draft.core.AvailablePlayers;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Draft;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.PagedInventoryMenu;
import tc.oc.pgm.util.text.TextTranslations;

public class PicksMenu extends PagedInventoryMenu {

  private static final int ROWS = 6;
  private static final int ITEMS_PER_PAGE = 28;

  private final TowersForPGM plugin;
  private final Draft draft;
  private final Captains captains;
  private final AvailablePlayers availablePlayers;
  private final Teams teams;
  private final int captainNumber;

  public PicksMenu(
      MatchPlayer viewer,
      TowersForPGM plugin,
      Draft draft,
      Captains captains,
      AvailablePlayers availablePlayers,
      Teams teams) {
    super(
        Component.translatable("draft.gui.title").color(NamedTextColor.DARK_GRAY),
        ROWS,
        viewer,
        null,
        ITEMS_PER_PAGE,
        1,
        1);
    this.plugin = plugin;
    this.draft = draft;
    this.captains = captains;
    this.availablePlayers = availablePlayers;
    this.teams = teams;
    this.captainNumber = captains.getCaptainTeam(viewer.getBukkit().getUniqueId());
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    if (Draft.getPhase() == DraftPhase.ENDED) {
      player.closeInventory();
      return;
    }
    fillBorder(contents, player);
    setupTeamInfoButton(contents, player);
    setupSuggestionButton(contents, player);
    setupPageContents(player, contents);
    fillBackground(contents);
  }

  @Override
  public void update(Player player, InventoryContents contents) {
    init(player, contents);
  }

  private void fillBackground(InventoryContents contents) {
    ItemStack background = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 7); // gris claro
    ItemMeta meta = background.getItemMeta();
    meta.setDisplayName(" ");
    background.setItemMeta(meta);
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < 9; col++) {
        if (contents.get(row, col).isEmpty()) {
          contents.set(row, col, ClickableItem.empty(background));
        }
      }
    }
  }

  private void fillBorder(InventoryContents contents, Player player) {
    ItemStack pane = getBorderGlass(player);

    // Fila superior y fila inferior completas
    for (int col = 0; col < 9; col++) {
      contents.set(0, col, ClickableItem.empty(pane));
      contents.set(lastRow(), col, ClickableItem.empty(pane));
    }

    // Columnas laterales en filas intermedias
    for (int row = 1; row < lastRow(); row++) {
      contents.set(row, 0, ClickableItem.empty(pane));
      contents.set(row, 8, ClickableItem.empty(pane));
    }
  }

  private ItemStack getBorderGlass(Player player) {
    UUID playerId = player.getUniqueId();
    boolean isCaptain = captains.isCaptain(playerId);

    // Recalcular teamNumber en tiempo real, no usar el campo del constructor
    int currentTeamNumber = teams.getTeamNumber(player.getName());

    boolean isSuggestion = !availablePlayers.hasAlreadySuggested(player.getName())
        && captains.isPlayerSuggestions()
        && ((currentTeamNumber == 1 && captains.isCaptain1Turn())
            || (currentTeamNumber == 2 && !captains.isCaptain1Turn()));

    boolean isTurn = (captainNumber == 1 && captains.isCaptain1Turn())
        || (captainNumber == 2 && !captains.isCaptain1Turn());

    byte data;
    if (isCaptain) {
      data = isTurn ? (byte) 5 : (byte) 14;
    } else if (isSuggestion) {
      data = (byte) 5;
    } else {
      data = (byte) 15;
    }

    ItemStack pane = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
    ItemMeta meta = pane.getItemMeta();
    meta.setDisplayName(" ");
    pane.setItemMeta(meta);
    return pane;
  }

  private void setupTeamInfoButton(InventoryContents contents, Player player) {
    MatchPlayer mp = PGM.get().getMatchManager().getPlayer(player);
    int teamNumber = teams.getTeamNumber(player.getName());
    boolean isAvailable = availablePlayers.getAllAvailablePlayers().contains(player.getName());

    ItemStack button = new ItemStack(Material.LEATHER_CHESTPLATE);
    LeatherArmorMeta meta = (LeatherArmorMeta) button.getItemMeta();

    meta.setColor(mp.getParty().getFullColor());

    meta.setDisplayName(TextTranslations.translateLegacy(
        Component.translatable(
                "draft.gui.availablePlayers",
                Component.text(availablePlayers.getAllAvailablePlayers().size()))
            .color(NamedTextColor.GREEN),
        player));

    Component loreComponent;

    if (teamNumber == -1 && !isAvailable) {
      loreComponent = Component.translatable("draft.gui.noAvailablePlayers");
    } else if (teamNumber == -1) {
      loreComponent = Component.translatable("draft.gui.noTeam");
    } else {
      loreComponent = Component.empty();
    }

    if (!loreComponent.equals(Component.empty())) {
      // El lore no admite componentes, así que traduzco con legacy a pesar de ser deprecado
      meta.setLore(List.of(TextTranslations.translateLegacy(loreComponent, player)));
    }

    button.setItemMeta(meta);
    contents.set(0, 4, ClickableItem.empty(button)); // centro de la fila superior
  }

  private void setupSuggestionButton(InventoryContents contents, Player player) {
    if (!captains.isCaptain(player.getUniqueId())) return;
    if (captainNumber != 1 && captainNumber != 2) return;
    if (teams.getAllTeam(captainNumber).size() <= 1) return;
    if (captains.isPlayerSuggestions()) return;

    boolean isTurn = (captainNumber == 1 && captains.isCaptain1Turn())
        || (captainNumber == 2 && !captains.isCaptain1Turn());
    if (!isTurn) return;
    // El lore no admite componentes, así que traduzco con legacy a pesar de ser deprecado
    String displayName = TextTranslations.translateLegacy(
        Component.translatable("draft.gui.suggestion").color(NamedTextColor.GREEN), player);
    String loreLine = TextTranslations.translateLegacy(
        Component.translatable("draft.gui.suggestion.lore").color(NamedTextColor.GRAY), player);
    ItemStack button = new ItemStack(Material.EMERALD_BLOCK);
    ItemMeta meta = button.getItemMeta();
    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
    meta.setDisplayName(displayName);
    meta.setLore(Arrays.asList(loreLine));
    button.setItemMeta(meta);

    contents.set(lastRow(), 4, ClickableItem.of(button, e -> {
      MatchPlayer mp = PGM.get().getMatchManager().getPlayer(player);
      draft.activatePlayerSuggestions(mp);
      player.closeInventory();
    }));
  }

  @Override
  public void setupPageContents(Player player, InventoryContents contents) {
    ClickableItem[] items = getPageContents(player);
    if (items != null && items.length != 0) {
      Pagination page = contents.pagination();
      page.setItems(items);
      page.setItemsPerPage(ITEMS_PER_PAGE);

      SlotIterator iterator = contents.newIterator(SlotIterator.Type.HORIZONTAL, 1, 1);

      // Blacklist borde superior e inferior
      for (int col = 0; col < 9; col++) {
        iterator.blacklist(SlotPos.of(0, col));
        iterator.blacklist(SlotPos.of(lastRow(), col));
      }
      // Blacklist columnas laterales en filas intermedias
      for (int row = 1; row < lastRow(); row++) {
        iterator.blacklist(SlotPos.of(row, 0));
        iterator.blacklist(SlotPos.of(row, 8));
      }

      page.addToIterator(iterator);

      if (!page.isFirst()) {
        contents.set(
            getPreviousPageSlot(), getPageItem(player, page.getPage() - 1, "menu.page.previous"));
      }
      if (!page.isLast()) {
        contents.set(getNextPageSlot(), getPageItem(player, page.getPage() + 1, "menu.page.next"));
      }
    } else {
      contents.set(getEmptyPageSlot(), getEmptyContentsButton(player));
    }
  }

  @Override
  public ClickableItem[] getPageContents(Player viewer) {
    return getSortedPlayerNames().stream()
        .map(name -> buildSkullItem(viewer, name))
        .toArray(ClickableItem[]::new);
  }

  private List<String> getSortedPlayerNames() {
    Set<String> nameSet = new HashSet<>();
    for (MatchPlayer p : availablePlayers.getAvailablePlayers()) {
      nameSet.add(p.getNameLegacy());
    }
    nameSet.addAll(availablePlayers.getAvailableOfflinePlayers());

    return nameSet.stream().sorted(String::compareToIgnoreCase).collect(Collectors.toList());
  }

  private ClickableItem buildSkullItem(Player viewer, String name) {
    boolean isStatsEnabled = plugin.getIsDatabaseActivated();
    Stats stats = isStatsEnabled ? availablePlayers.getStatsForPlayer(name) : null;
    PlayerSkullItem skullItem = new PlayerSkullItem(name, stats, captains, availablePlayers, teams);
    return ClickableItem.of(skullItem.createItem(viewer), e -> handleSkullClick(viewer, name));
  }

  private void handleSkullClick(Player player, String name) {
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    DraftPhase phase = Draft.getPhase();

    if (!captains.isCaptain(player.getUniqueId())) {
      boolean success = draft.suggestPlayer(matchPlayer, name);
      if (success) {
        player.closeInventory();
      }
      return;
    }

    if (phase == DraftPhase.CAPTAINS || phase == DraftPhase.REROLL) {
      return;
    }

    if (captainNumber == -1) {
      return;
    }

    boolean myTurn = (captains.isCaptain1Turn() && captainNumber == 1)
        || (!captains.isCaptain1Turn() && captainNumber == 2);
    if (!myTurn) {
      return;
    }

    Component error = validatePick(name);
    if (error != null) {
      player.closeInventory();
      matchPlayer.sendWarning(error);
      return;
    }

    draft.pickPlayer(name);
    player.closeInventory();
  }

  private Component validatePick(String name) {
    boolean inOnlineList = availablePlayers.getAvailablePlayers().stream()
        .anyMatch(p -> p.getNameLegacy().equalsIgnoreCase(name));
    boolean inOfflineList = availablePlayers.getAvailableOfflinePlayers().stream()
        .anyMatch(n -> n.equalsIgnoreCase(name));

    if (!inOnlineList && !inOfflineList)
      return Component.translatable("draft.picks.notInList", MatchManager.getPrefixedName(name));

    if (teams.isPlayerInAnyTeam(name))
      return Component.translatable(
          "draft.picks.alreadyPicked", MatchManager.getPrefixedName(name));

    return null;
  }

  @Override
  public SlotPos getEmptyPageSlot() {
    return SlotPos.of(3, 4);
  }

  @Override
  public SlotPos getPreviousPageSlot() {
    return SlotPos.of(lastRow(), 3);
  }

  @Override
  public SlotPos getNextPageSlot() {
    return SlotPos.of(lastRow(), 5);
  }
}
