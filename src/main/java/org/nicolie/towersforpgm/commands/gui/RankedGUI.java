package org.nicolie.towersforpgm.commands.gui;

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
import org.nicolie.towersforpgm.commands.commandUtils.RankedConfig;
import org.nicolie.towersforpgm.configs.tables.TableType;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.utils.LanguageManager;

public class RankedGUI implements Listener {
  private static TowersForPGM plugin = TowersForPGM.getInstance();
  private static final int BACK_SLOT = 0;
  private static final int MIN_SIZE_SLOT = 10;
  private static final int MAX_SIZE_SLOT = 11;
  private static final int ORDER_SLOT = 12;
  private static final int MATCHMAKING_SLOT = 13;
  private static final int DEFAULT_TABLE_SLOT = 14;
  private static final int ADD_TABLE_SLOT = 15;
  private static final int REMOVE_TABLE_SLOT = 16;
  private static final int ADD_MAP_SLOT = 19;
  private static final int REMOVE_MAP_SLOT = 20;
  private static final int TABLES_SLOT = 21;
  private static final int MAPS_SLOT = 22;
  private static final int VOICECHAT_SLOT = 25;

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
    Inventory gui = Bukkit.createInventory(null, 36, LanguageManager.message("gui.ranked.title"));

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
    backMeta.setDisplayName(LanguageManager.message("gui.back"));
    backItem.setItemMeta(backMeta);
    gui.setItem(BACK_SLOT, backItem);

    // ===== BASIC CONFIGURATION =====
    // Ranked Min Size
    ItemStack minSizeItem = new ItemStack(Material.GOLD_NUGGET);
    ItemMeta minSizeMeta = minSizeItem.getItemMeta();
    minSizeMeta.setDisplayName(LanguageManager.message("gui.ranked.minSize.title"));
    List<String> minSizeLore = new ArrayList<>();
    minSizeLore.add(LanguageManager.message("gui.ranked.minSize.current")
        .replace("{value}", String.valueOf(plugin.config().ranked().getRankedMinSize())));
    minSizeLore.add("§7");
    minSizeLore.add(LanguageManager.message("gui.ranked.minSize.lore1"));
    minSizeLore.add(LanguageManager.message("gui.ranked.minSize.lore2"));
    minSizeMeta.setLore(minSizeLore);
    minSizeItem.setItemMeta(minSizeMeta);
    gui.setItem(MIN_SIZE_SLOT, minSizeItem);

    // Ranked Max Size
    ItemStack maxSizeItem = new ItemStack(Material.DIAMOND);
    ItemMeta maxSizeMeta = maxSizeItem.getItemMeta();
    maxSizeMeta.setDisplayName(LanguageManager.message("gui.ranked.maxSize.title"));
    List<String> maxSizeLore = new ArrayList<>();
    maxSizeLore.add(LanguageManager.message("gui.ranked.maxSize.current")
        .replace("{value}", String.valueOf(plugin.config().ranked().getRankedMaxSize())));
    maxSizeLore.add("§7");
    maxSizeLore.add(LanguageManager.message("gui.ranked.maxSize.lore1"));
    maxSizeLore.add(LanguageManager.message("gui.ranked.maxSize.lore2"));
    maxSizeMeta.setLore(maxSizeLore);
    maxSizeItem.setItemMeta(maxSizeMeta);
    gui.setItem(MAX_SIZE_SLOT, maxSizeItem);

    // Ranked Order
    ItemStack orderItem = new ItemStack(Material.PAPER);
    ItemMeta orderMeta = orderItem.getItemMeta();
    orderMeta.setDisplayName(LanguageManager.message("gui.ranked.order.title"));
    List<String> orderLore = new ArrayList<>();
    orderLore.add(LanguageManager.message("gui.ranked.order.current")
        .replace("{value}", plugin.config().ranked().getRankedOrder()));
    orderLore.add("§7");
    orderLore.add(LanguageManager.message("gui.ranked.order.lore1"));
    orderLore.add(LanguageManager.message("gui.ranked.order.lore2"));
    orderMeta.setLore(orderLore);
    orderItem.setItemMeta(orderMeta);
    gui.setItem(ORDER_SLOT, orderItem);

    // Matchmaking Toggle
    ItemStack matchmakingItem = new ItemStack(
        plugin.config().ranked().isRankedMatchmaking() ? Material.EMERALD : Material.REDSTONE);
    ItemMeta matchmakingMeta = matchmakingItem.getItemMeta();
    matchmakingMeta.setDisplayName(LanguageManager.message("gui.ranked.matchmaking.title"));
    List<String> matchmakingLore = new ArrayList<>();
    matchmakingLore.add(LanguageManager.message("gui.ranked.matchmaking.current")
        .replace(
            "{status}",
            plugin.config().ranked().isRankedMatchmaking()
                ? LanguageManager.message("gui.status.enabled")
                : LanguageManager.message("gui.status.disabled")));
    matchmakingLore.add("§7");
    matchmakingLore.add(LanguageManager.message("gui.ranked.matchmaking.lore1"));
    matchmakingLore.add(LanguageManager.message("gui.ranked.matchmaking.lore2"));
    matchmakingMeta.setLore(matchmakingLore);
    matchmakingItem.setItemMeta(matchmakingMeta);
    gui.setItem(MATCHMAKING_SLOT, matchmakingItem);

    // ===== TABLE MANAGEMENT =====
    // Current Profile Table (Read-only)
    ItemStack defaultTableItem = new ItemStack(Material.ENCHANTED_BOOK);
    ItemMeta defaultTableMeta = defaultTableItem.getItemMeta();
    defaultTableMeta.setDisplayName(LanguageManager.message("gui.ranked.default_table.title"));
    List<String> defaultTableLore = new ArrayList<>();
    String defaultTable = plugin.config().databaseTables().getRankedDefaultTable();
    String activeProfileName = plugin.config().ranked().getActiveProfileName();
    defaultTableLore.add(LanguageManager.message("gui.ranked.default_table.current")
        .replace(
            "{table}",
            defaultTable != null ? defaultTable : LanguageManager.message("gui.status.none")));
    defaultTableLore.add("§7");
    defaultTableLore.add("§7"
        + LanguageManager.message("gui.ranked.default_table.profile")
            .replace("{profile}", activeProfileName));
    defaultTableLore.add("§7" + LanguageManager.message("gui.ranked.default_table.readonly"));
    defaultTableMeta.setLore(defaultTableLore);
    defaultTableItem.setItemMeta(defaultTableMeta);
    gui.setItem(DEFAULT_TABLE_SLOT, defaultTableItem);

    // Add Table
    ItemStack addTableItem = new ItemStack(Material.EMERALD_BLOCK);
    ItemMeta addTableMeta = addTableItem.getItemMeta();
    addTableMeta.setDisplayName(LanguageManager.message("gui.ranked.add_table.title"));
    List<String> addTableLore = new ArrayList<>();
    addTableLore.add(LanguageManager.message("gui.ranked.add_table.lore"));
    addTableMeta.setLore(addTableLore);
    addTableItem.setItemMeta(addTableMeta);
    gui.setItem(ADD_TABLE_SLOT, addTableItem);

    // Remove Table
    ItemStack removeTableItem = new ItemStack(Material.REDSTONE_BLOCK);
    ItemMeta removeTableMeta = removeTableItem.getItemMeta();
    removeTableMeta.setDisplayName(LanguageManager.message("gui.ranked.remove_table.title"));
    List<String> removeTableLore = new ArrayList<>();
    removeTableLore.add(LanguageManager.message("gui.ranked.remove_table.lore"));
    removeTableMeta.setLore(removeTableLore);
    removeTableItem.setItemMeta(removeTableMeta);
    gui.setItem(REMOVE_TABLE_SLOT, removeTableItem);

    // ===== MAP MANAGEMENT =====
    // Add Current Map
    ItemStack addMapItem = new ItemStack(Material.GRASS);
    ItemMeta addMapMeta = addMapItem.getItemMeta();
    addMapMeta.setDisplayName(LanguageManager.message("gui.ranked.add_map.title"));
    List<String> addMapLore = new ArrayList<>();
    addMapLore.add(LanguageManager.message("gui.ranked.add_map.lore"));
    addMapMeta.setLore(addMapLore);
    addMapItem.setItemMeta(addMapMeta);
    gui.setItem(ADD_MAP_SLOT, addMapItem);

    // Remove Current Map
    ItemStack removeMapItem = new ItemStack(Material.DEAD_BUSH);
    ItemMeta removeMapMeta = removeMapItem.getItemMeta();
    removeMapMeta.setDisplayName(LanguageManager.message("gui.ranked.remove_map.title"));
    List<String> removeMapLore = new ArrayList<>();
    removeMapLore.add(LanguageManager.message("gui.ranked.remove_map.lore"));
    removeMapMeta.setLore(removeMapLore);
    removeMapItem.setItemMeta(removeMapMeta);
    gui.setItem(REMOVE_MAP_SLOT, removeMapItem);

    // ===== INFORMATION DISPLAY =====
    // Ranked Tables List
    ItemStack tablesItem = new ItemStack(Material.BOOK);
    ItemMeta tablesMeta = tablesItem.getItemMeta();
    tablesMeta.setDisplayName(LanguageManager.message("gui.ranked.tables.title"));
    List<String> tablesLore = new ArrayList<>();
    tablesLore.add(LanguageManager.message("gui.ranked.tables.current")
        .replace(
            "{count}",
            String.valueOf(
                plugin.config().databaseTables().getTables(TableType.RANKED).size())));
    tablesLore.add("§7");
    for (String table : plugin.config().databaseTables().getTables(TableType.RANKED)) {
      tablesLore.add(LanguageManager.message("gui.ranked.tables.item").replace("{table}", table));
    }
    if (plugin.config().databaseTables().getTables(TableType.RANKED).isEmpty()) {
      tablesLore.add(LanguageManager.message("gui.status.none"));
    }
    tablesMeta.setLore(tablesLore);
    tablesItem.setItemMeta(tablesMeta);
    gui.setItem(TABLES_SLOT, tablesItem);

    // Ranked Maps List
    ItemStack mapsItem = new ItemStack(Material.MAP);
    ItemMeta mapsMeta = mapsItem.getItemMeta();
    mapsMeta.setDisplayName(LanguageManager.message("gui.ranked.maps.title"));
    List<String> mapsLore = new ArrayList<>();
    mapsLore.add(LanguageManager.message("gui.ranked.maps.current")
        .replace(
            "{count}", String.valueOf(plugin.config().ranked().getRankedMaps().size())));
    mapsLore.add("§7");
    for (String map : plugin.config().ranked().getRankedMaps()) {
      mapsLore.add(LanguageManager.message("gui.ranked.maps.item").replace("{map}", map));
    }
    if (plugin.config().ranked().getRankedMaps().isEmpty()) {
      mapsLore.add(LanguageManager.message("gui.status.none"));
    }
    mapsMeta.setLore(mapsLore);
    mapsItem.setItemMeta(mapsMeta);
    gui.setItem(MAPS_SLOT, mapsItem);

    // VoiceChat Toggle (only if MatchBot is enabled)
    if (TowersForPGM.getInstance().isMatchBotEnabled()) {
      ItemStack voiceChatItem =
          new ItemStack(MatchBotConfig.isVoiceChatEnabled() ? Material.EMERALD : Material.REDSTONE);
      ItemMeta voiceChatMeta = voiceChatItem.getItemMeta();
      voiceChatMeta.setDisplayName(LanguageManager.message("gui.ranked.voicechat.title"));
      List<String> voiceChatLore = new ArrayList<>();
      voiceChatLore.add(LanguageManager.message("gui.ranked.voicechat.current")
          .replace(
              "{status}",
              MatchBotConfig.isVoiceChatEnabled()
                  ? LanguageManager.message("gui.status.enabled")
                  : LanguageManager.message("gui.status.disabled")));
      voiceChatLore.add("§7");
      voiceChatLore.add(LanguageManager.message("gui.ranked.voicechat.lore1"));
      voiceChatLore.add(LanguageManager.message("gui.ranked.voicechat.lore2"));
      voiceChatMeta.setLore(voiceChatLore);
      voiceChatItem.setItemMeta(voiceChatMeta);
      gui.setItem(VOICECHAT_SLOT, voiceChatItem);
    }

    player.openInventory(gui);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) return;
    Player player = (Player) event.getWhoClicked();

    if (event.getView().getTitle().equals(LanguageManager.message("gui.ranked.title"))) {
      event.setCancelled(true);

      ItemStack clicked = event.getCurrentItem();
      if (clicked == null || clicked.getType() == Material.AIR) return;

      switch (event.getSlot()) {
        case BACK_SLOT: // Back to main menu
          mainGUI.openMainMenu(player);
          break;
        case MIN_SIZE_SLOT: // Ranked Min Size
          player.closeInventory();
          player.sendMessage(LanguageManager.message("gui.ranked.minSize.input"));
          waitingForInput.put(player.getUniqueId(), "min_size");
          break;
        case MAX_SIZE_SLOT: // Ranked Max Size
          player.closeInventory();
          player.sendMessage(LanguageManager.message("gui.ranked.maxSize.input"));
          waitingForInput.put(player.getUniqueId(), "max_size");
          break;
        case ORDER_SLOT: // Ranked Order
          player.closeInventory();
          player.sendMessage(LanguageManager.message("gui.ranked.order.input"));
          waitingForInput.put(player.getUniqueId(), "order");
          break;
        case MATCHMAKING_SLOT: // Toggle Matchmaking
          rankedConfig.matchmaking(player, !plugin.config().ranked().isRankedMatchmaking());
          openRankedMenu(player); // Refresh the GUI
          break;
        case ADD_TABLE_SLOT: // Add Table
          player.closeInventory();
          player.sendMessage(LanguageManager.message("gui.ranked.add_table.input"));
          waitingForInput.put(player.getUniqueId(), "add_table");
          break;
        case REMOVE_TABLE_SLOT: // Remove Table
          player.closeInventory();
          player.sendMessage(LanguageManager.message("gui.ranked.remove_table.input"));
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
        case VOICECHAT_SLOT: // Toggle VoiceChat (only if MatchBot is enabled)
          if (TowersForPGM.getInstance().isMatchBotEnabled()) {
            MatchBotConfig.setVoiceChatEnabled(!MatchBotConfig.isVoiceChatEnabled());
            openRankedMenu(player); // Refresh the GUI
          }
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
          case "min_size":
            rankedConfig.minSize(player, input);
            break;
          case "max_size":
            rankedConfig.maxSize(player, input);
            break;
          case "order":
            rankedConfig.draftOrder(player, input);
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
