package org.nicolie.towersforpgm.gui.towersCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.commandUtils.RankedConfig;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;

public class RankedGUI implements Listener {
  private static final int BACK_SLOT = 0;
  private static final int SIZE_SLOT = 10;
  private static final int ORDER_SLOT = 11;
  private static final int MATCHMAKING_SLOT = 12;
  private static final int DEFAULT_TABLE_SLOT = 14;
  private static final int ADD_TABLE_SLOT = 15;
  private static final int REMOVE_TABLE_SLOT = 16;
  private static final int ADD_MAP_SLOT = 19;
  private static final int REMOVE_MAP_SLOT = 21;
  private static final int TABLES_SLOT = 23;
  private static final int MAPS_SLOT = 25;

  private static boolean eventsRegistered = false;
  private final RankedConfig rankedConfig;
  private final TowersConfigGUI mainGUI;
  private final Map<UUID, String> waitingForInput = new HashMap<>();

  public RankedGUI(RankedConfig rankedConfig, TowersConfigGUI mainGUI) {
    this.rankedConfig = rankedConfig;
    this.mainGUI = mainGUI;

    // Registrar eventos solo una vez para esta clase
    if (!eventsRegistered) {
      Bukkit.getPluginManager().registerEvents(this, TowersForPGM.getInstance());
      eventsRegistered = true;
    }
  }

  public void openRankedMenu(Player player) {
    Inventory gui =
        Bukkit.createInventory(null, 36, LanguageManager.langMessage("gui.ranked.title"));

    // Gray glass border
    ItemStack grayGlass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
    ItemMeta grayMeta = grayGlass.getItemMeta();
    grayMeta.setDisplayName(" ");
    grayGlass.setItemMeta(grayMeta);

    // Fill border with gray glass
    for (int i = 0; i < 36; i++) {
      if (i < 9 || i > 26 || i % 9 == 0 || i % 9 == 8) {
        gui.setItem(i, grayGlass);
      }
    }

    // ===== NAVIGATION =====
    ItemStack backItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
    ItemMeta backMeta = backItem.getItemMeta();
    backMeta.setDisplayName(LanguageManager.langMessage("gui.back"));
    backItem.setItemMeta(backMeta);
    gui.setItem(BACK_SLOT, backItem);

    // ===== BASIC CONFIGURATION =====
    // Ranked Size
    ItemStack sizeItem = new ItemStack(Material.GOLD_NUGGET);
    ItemMeta sizeMeta = sizeItem.getItemMeta();
    sizeMeta.setDisplayName(LanguageManager.langMessage("gui.ranked.size.title"));
    List<String> sizeLore = new ArrayList<>();
    sizeLore.add(LanguageManager.langMessage("gui.ranked.size.current")
        .replace("{value}", String.valueOf(ConfigManager.getRankedSize())));
    sizeLore.add("§7");
    sizeLore.add(LanguageManager.langMessage("gui.ranked.size.lore1"));
    sizeLore.add(LanguageManager.langMessage("gui.ranked.size.lore2"));
    sizeMeta.setLore(sizeLore);
    sizeItem.setItemMeta(sizeMeta);
    gui.setItem(SIZE_SLOT, sizeItem);

    // Ranked Order
    ItemStack orderItem = new ItemStack(Material.PAPER);
    ItemMeta orderMeta = orderItem.getItemMeta();
    orderMeta.setDisplayName(LanguageManager.langMessage("gui.ranked.order.title"));
    List<String> orderLore = new ArrayList<>();
    orderLore.add(LanguageManager.langMessage("gui.ranked.order.current")
        .replace("{value}", ConfigManager.getRankedOrder()));
    orderLore.add("§7");
    orderLore.add(LanguageManager.langMessage("gui.ranked.order.lore1"));
    orderLore.add(LanguageManager.langMessage("gui.ranked.order.lore2"));
    orderMeta.setLore(orderLore);
    orderItem.setItemMeta(orderMeta);
    gui.setItem(ORDER_SLOT, orderItem);

    // Matchmaking Toggle
    ItemStack matchmakingItem =
        new ItemStack(ConfigManager.isRankedMatchmaking() ? Material.EMERALD : Material.REDSTONE);
    ItemMeta matchmakingMeta = matchmakingItem.getItemMeta();
    matchmakingMeta.setDisplayName(LanguageManager.langMessage("gui.ranked.matchmaking.title"));
    List<String> matchmakingLore = new ArrayList<>();
    matchmakingLore.add(LanguageManager.langMessage("gui.ranked.matchmaking.current")
        .replace(
            "{status}",
            ConfigManager.isRankedMatchmaking()
                ? LanguageManager.langMessage("gui.status.enabled")
                : LanguageManager.langMessage("gui.status.disabled")));
    matchmakingLore.add("§7");
    matchmakingLore.add(LanguageManager.langMessage("gui.ranked.matchmaking.lore1"));
    matchmakingLore.add(LanguageManager.langMessage("gui.ranked.matchmaking.lore2"));
    matchmakingMeta.setLore(matchmakingLore);
    matchmakingItem.setItemMeta(matchmakingMeta);
    gui.setItem(MATCHMAKING_SLOT, matchmakingItem);

    // ===== TABLE MANAGEMENT =====
    // Default Table
    ItemStack defaultTableItem = new ItemStack(Material.DIAMOND);
    ItemMeta defaultTableMeta = defaultTableItem.getItemMeta();
    defaultTableMeta.setDisplayName(LanguageManager.langMessage("gui.ranked.default_table.title"));
    List<String> defaultTableLore = new ArrayList<>();
    String defaultTable = ConfigManager.getRankedDefaultTable();
    defaultTableLore.add(LanguageManager.langMessage("gui.ranked.default_table.current")
        .replace(
            "{table}",
            defaultTable != null ? defaultTable : LanguageManager.langMessage("gui.status.none")));
    defaultTableLore.add("§7");
    defaultTableLore.add(LanguageManager.langMessage("gui.ranked.default_table.lore1"));
    defaultTableLore.add(LanguageManager.langMessage("gui.ranked.default_table.lore2"));
    defaultTableMeta.setLore(defaultTableLore);
    defaultTableItem.setItemMeta(defaultTableMeta);
    gui.setItem(DEFAULT_TABLE_SLOT, defaultTableItem);

    // Add Table
    ItemStack addTableItem = new ItemStack(Material.EMERALD_BLOCK);
    ItemMeta addTableMeta = addTableItem.getItemMeta();
    addTableMeta.setDisplayName(LanguageManager.langMessage("gui.ranked.add_table.title"));
    List<String> addTableLore = new ArrayList<>();
    addTableLore.add(LanguageManager.langMessage("gui.ranked.add_table.lore"));
    addTableMeta.setLore(addTableLore);
    addTableItem.setItemMeta(addTableMeta);
    gui.setItem(ADD_TABLE_SLOT, addTableItem);

    // Remove Table
    ItemStack removeTableItem = new ItemStack(Material.REDSTONE_BLOCK);
    ItemMeta removeTableMeta = removeTableItem.getItemMeta();
    removeTableMeta.setDisplayName(LanguageManager.langMessage("gui.ranked.remove_table.title"));
    List<String> removeTableLore = new ArrayList<>();
    removeTableLore.add(LanguageManager.langMessage("gui.ranked.remove_table.lore"));
    removeTableMeta.setLore(removeTableLore);
    removeTableItem.setItemMeta(removeTableMeta);
    gui.setItem(REMOVE_TABLE_SLOT, removeTableItem);

    // ===== MAP MANAGEMENT =====
    // Add Current Map
    ItemStack addMapItem = new ItemStack(Material.GRASS);
    ItemMeta addMapMeta = addMapItem.getItemMeta();
    addMapMeta.setDisplayName(LanguageManager.langMessage("gui.ranked.add_map.title"));
    List<String> addMapLore = new ArrayList<>();
    addMapLore.add(LanguageManager.langMessage("gui.ranked.add_map.lore"));
    addMapMeta.setLore(addMapLore);
    addMapItem.setItemMeta(addMapMeta);
    gui.setItem(ADD_MAP_SLOT, addMapItem);

    // Remove Current Map
    ItemStack removeMapItem = new ItemStack(Material.DEAD_BUSH);
    ItemMeta removeMapMeta = removeMapItem.getItemMeta();
    removeMapMeta.setDisplayName(LanguageManager.langMessage("gui.ranked.remove_map.title"));
    List<String> removeMapLore = new ArrayList<>();
    removeMapLore.add(LanguageManager.langMessage("gui.ranked.remove_map.lore"));
    removeMapMeta.setLore(removeMapLore);
    removeMapItem.setItemMeta(removeMapMeta);
    gui.setItem(REMOVE_MAP_SLOT, removeMapItem);

    // ===== INFORMATION DISPLAY =====
    // Ranked Tables List
    ItemStack tablesItem = new ItemStack(Material.BOOK);
    ItemMeta tablesMeta = tablesItem.getItemMeta();
    tablesMeta.setDisplayName(LanguageManager.langMessage("gui.ranked.tables.title"));
    List<String> tablesLore = new ArrayList<>();
    tablesLore.add(LanguageManager.langMessage("gui.ranked.tables.current")
        .replace("{count}", String.valueOf(ConfigManager.getRankedTables().size())));
    tablesLore.add("§7");
    for (String table : ConfigManager.getRankedTables()) {
      tablesLore.add(
          LanguageManager.langMessage("gui.ranked.tables.item").replace("{table}", table));
    }
    if (ConfigManager.getRankedTables().isEmpty()) {
      tablesLore.add(LanguageManager.langMessage("gui.status.none"));
    }
    tablesMeta.setLore(tablesLore);
    tablesItem.setItemMeta(tablesMeta);
    gui.setItem(TABLES_SLOT, tablesItem);

    // Ranked Maps List
    ItemStack mapsItem = new ItemStack(Material.MAP);
    ItemMeta mapsMeta = mapsItem.getItemMeta();
    mapsMeta.setDisplayName(LanguageManager.langMessage("gui.ranked.maps.title"));
    List<String> mapsLore = new ArrayList<>();
    mapsLore.add(LanguageManager.langMessage("gui.ranked.maps.current")
        .replace("{count}", String.valueOf(ConfigManager.getRankedMaps().size())));
    mapsLore.add("§7");
    for (String map : ConfigManager.getRankedMaps()) {
      mapsLore.add(LanguageManager.langMessage("gui.ranked.maps.item").replace("{map}", map));
    }
    if (ConfigManager.getRankedMaps().isEmpty()) {
      mapsLore.add(LanguageManager.langMessage("gui.status.none"));
    }
    mapsMeta.setLore(mapsLore);
    mapsItem.setItemMeta(mapsMeta);
    gui.setItem(MAPS_SLOT, mapsItem);

    player.openInventory(gui);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) return;
    Player player = (Player) event.getWhoClicked();

    if (event.getView().getTitle().equals(LanguageManager.langMessage("gui.ranked.title"))) {
      event.setCancelled(true);

      ItemStack clicked = event.getCurrentItem();
      if (clicked == null || clicked.getType() == Material.AIR) return;

      switch (event.getSlot()) {
        case BACK_SLOT: // Back to main menu
          mainGUI.openMainMenu(player);
          break;
        case SIZE_SLOT: // Ranked Size
          player.closeInventory();
          player.sendMessage(LanguageManager.langMessage("gui.ranked.size.input"));
          waitingForInput.put(player.getUniqueId(), "size");
          break;
        case ORDER_SLOT: // Ranked Order
          player.closeInventory();
          player.sendMessage(LanguageManager.langMessage("gui.ranked.order.input"));
          waitingForInput.put(player.getUniqueId(), "order");
          break;
        case MATCHMAKING_SLOT: // Toggle Matchmaking
          rankedConfig.matchmaking(player, !ConfigManager.isRankedMatchmaking());
          openRankedMenu(player); // Refresh the GUI
          break;
        case DEFAULT_TABLE_SLOT: // Set Default Table
          player.closeInventory();
          player.sendMessage(LanguageManager.langMessage("gui.ranked.default_table.input"));
          waitingForInput.put(player.getUniqueId(), "default_table");
          break;
        case ADD_TABLE_SLOT: // Add Table
          player.closeInventory();
          player.sendMessage(LanguageManager.langMessage("gui.ranked.add_table.input"));
          waitingForInput.put(player.getUniqueId(), "add_table");
          break;
        case REMOVE_TABLE_SLOT: // Remove Table
          player.closeInventory();
          player.sendMessage(LanguageManager.langMessage("gui.ranked.remove_table.input"));
          waitingForInput.put(player.getUniqueId(), "remove_table");
          break;
        case ADD_MAP_SLOT: // Add Current Map
          rankedConfig.addMap(player);
          openRankedMenu(player); // Refresh the GUI
          break;
        case REMOVE_MAP_SLOT: // Remove Current Map
          rankedConfig.removeMap(player);
          openRankedMenu(player); // Refresh the GUI
          break;
        case TABLES_SLOT: // View Tables (no action, info display only)
          break;
        case MAPS_SLOT: // View Maps (no action, info display only)
          break;
      }
    }
  }

  @EventHandler
  public void onPlayerChat(AsyncPlayerChatEvent event) {
    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();

    if (waitingForInput.containsKey(playerId)) {
      event.setCancelled(true);
      String inputType = waitingForInput.remove(playerId);
      String input = event.getMessage();

      Bukkit.getScheduler().runTask(TowersForPGM.getInstance(), () -> {
        switch (inputType) {
          case "size":
            rankedConfig.size(player, input);
            break;
          case "order":
            rankedConfig.draftOrder(player, input);
            break;
          case "default_table":
            rankedConfig.setDefaultTable(player, input);
            break;
          case "add_table":
            rankedConfig.addTable(player, input);
            break;
          case "remove_table":
            rankedConfig.deleteTable(player, input);
            break;
        }

        // Reopen the GUI after processing
        Bukkit.getScheduler()
            .runTaskLater(
                TowersForPGM.getInstance(),
                () -> {
                  openRankedMenu(player);
                },
                1L);
      });
    }
  }
}
