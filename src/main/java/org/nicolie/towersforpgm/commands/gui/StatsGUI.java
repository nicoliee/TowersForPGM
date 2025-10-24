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
import org.nicolie.towersforpgm.commands.commandUtils.StatsConfig;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.MatchManager;

public class StatsGUI implements Listener {
  private static final int BACK_SLOT = 0;
  private static final int DEFAULT_TABLE_SLOT = 9;
  private static final int ADD_TABLE_SLOT = 10;
  private static final int REMOVE_TABLE_SLOT = 11;
  private static final int LIST_TABLES_SLOT = 12;
  private static final int ADD_MAP_TABLE_SLOT = 14;
  private static final int REMOVE_MAP_TABLE_SLOT = 15;
  private static final int ADD_TEMP_TABLE_SLOT = 16;
  private static final int REMOVE_TEMP_TABLE_SLOT = 17;

  private final StatsConfig statsConfig;
  private final TowersConfigGUI mainGUI;
  private final Map<UUID, String> waitingForInput = new HashMap<>();
  private boolean isRegistered = false;

  public StatsGUI(StatsConfig statsConfig, TowersConfigGUI mainGUI) {
    this.statsConfig = statsConfig;
    this.mainGUI = mainGUI;

    // Registrar eventos solo una vez para esta instancia
    if (!isRegistered) {
      Bukkit.getPluginManager().registerEvents(this, TowersForPGM.getInstance());
      isRegistered = true;
    }
  }

  public void openStatsMenu(Player player) {
    Inventory gui =
        Bukkit.createInventory(null, 27, LanguageManager.langMessage("gui.stats.title"));

    // Gray glass border
    ItemStack grayGlass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
    ItemMeta grayMeta = grayGlass.getItemMeta();
    grayMeta.setDisplayName(" ");
    grayGlass.setItemMeta(grayMeta);

    // Fill border with gray glass
    for (int i = 0; i < 27; i++) {
      if (i < 9 || i > 17 || i % 9 == 0 || i % 9 == 8) {
        gui.setItem(i, grayGlass);
      }
    }

    // ===== NAVIGATION =====
    ItemStack backItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
    ItemMeta backMeta = backItem.getItemMeta();
    backMeta.setDisplayName(LanguageManager.langMessage("gui.back"));
    backItem.setItemMeta(backMeta);
    gui.setItem(BACK_SLOT, backItem);

    // ===== GLOBAL TABLE MANAGEMENT =====
    // Set default table
    ItemStack defaultItem = new ItemStack(Material.EMERALD);
    ItemMeta defaultMeta = defaultItem.getItemMeta();
    defaultMeta.setDisplayName(LanguageManager.langMessage("gui.stats.default.title"));
    List<String> defaultLore = new ArrayList<>();
    defaultLore.add(LanguageManager.langMessage("gui.stats.default.lore"));
    defaultMeta.setLore(defaultLore);
    defaultItem.setItemMeta(defaultMeta);
    gui.setItem(DEFAULT_TABLE_SLOT, defaultItem);

    // Add table
    ItemStack addItem = new ItemStack(Material.EMERALD_BLOCK);
    ItemMeta addMeta = addItem.getItemMeta();
    addMeta.setDisplayName(LanguageManager.langMessage("gui.stats.add.title"));
    List<String> addLore = new ArrayList<>();
    addLore.add(LanguageManager.langMessage("gui.stats.add.lore"));
    addMeta.setLore(addLore);
    addItem.setItemMeta(addMeta);
    gui.setItem(ADD_TABLE_SLOT, addItem);

    // Remove table
    ItemStack removeItem = new ItemStack(Material.REDSTONE_BLOCK);
    ItemMeta removeMeta = removeItem.getItemMeta();
    removeMeta.setDisplayName(LanguageManager.langMessage("gui.stats.remove.title"));
    List<String> removeLore = new ArrayList<>();
    removeLore.add(LanguageManager.langMessage("gui.stats.remove.lore"));
    removeMeta.setLore(removeLore);
    removeItem.setItemMeta(removeMeta);
    gui.setItem(REMOVE_TABLE_SLOT, removeItem);

    // List tables
    ItemStack listItem = new ItemStack(Material.BOOK);
    ItemMeta listMeta = listItem.getItemMeta();
    listMeta.setDisplayName(LanguageManager.langMessage("gui.stats.list.title"));
    List<String> listLore = new ArrayList<>();
    listLore.add(LanguageManager.langMessage("gui.stats.list.lore"));
    List<String> tables = ConfigManager.getTables();
    listLore.add(LanguageManager.langMessage("gui.stats.list.count")
        .replace("{count}", String.valueOf(tables.size())));
    String currentTable =
        ConfigManager.getActiveTable(MatchManager.getMatch().getMap().getName());
    for (String table : tables) {
      String displayTable = table;
      if (currentTable != null && currentTable.equals(table)) {
        displayTable = "§e" + table + "§7";
      }
      listLore.add(
          LanguageManager.langMessage("gui.stats.list.item").replace("{table}", displayTable));
    }
    listMeta.setLore(listLore);
    listItem.setItemMeta(listMeta);
    gui.setItem(LIST_TABLES_SLOT, listItem);

    // ===== MAP-SPECIFIC TABLES =====
    // Add map-specific table
    ItemStack addMapItem = new ItemStack(Material.MAP);
    ItemMeta addMapMeta = addMapItem.getItemMeta();
    addMapMeta.setDisplayName(LanguageManager.langMessage("gui.stats.add_map.title"));
    List<String> addMapLore = new ArrayList<>();
    addMapLore.add(LanguageManager.langMessage("gui.stats.add_map.lore"));
    addMapMeta.setLore(addMapLore);
    addMapItem.setItemMeta(addMapMeta);
    gui.setItem(ADD_MAP_TABLE_SLOT, addMapItem);

    // Remove map-specific table
    ItemStack removeMapItem = new ItemStack(Material.BARRIER);
    ItemMeta removeMapMeta = removeMapItem.getItemMeta();
    removeMapMeta.setDisplayName(LanguageManager.langMessage("gui.stats.remove_map.title"));
    List<String> removeMapLore = new ArrayList<>();
    removeMapLore.add(LanguageManager.langMessage("gui.stats.remove_map.lore"));
    removeMapMeta.setLore(removeMapLore);
    removeMapItem.setItemMeta(removeMapMeta);
    gui.setItem(REMOVE_MAP_TABLE_SLOT, removeMapItem);

    // ===== TEMPORARY TABLES =====
    // Add temporary table
    ItemStack addTempItem = new ItemStack(Material.PAPER);
    ItemMeta addTempMeta = addTempItem.getItemMeta();
    addTempMeta.setDisplayName(LanguageManager.langMessage("gui.stats.add_temp.title"));
    List<String> addTempLore = new ArrayList<>();
    addTempLore.add(LanguageManager.langMessage("gui.stats.add_temp.lore"));
    String tempTable = ConfigManager.getTemp();
    addTempLore.add(LanguageManager.langMessage("gui.stats.add_temp.current")
        .replace(
            "{table}",
            tempTable != null ? tempTable : LanguageManager.langMessage("gui.stats.none")));
    addTempMeta.setLore(addTempLore);
    addTempItem.setItemMeta(addTempMeta);
    gui.setItem(ADD_TEMP_TABLE_SLOT, addTempItem);

    // Remove temporary table
    ItemStack removeTempItem = new ItemStack(Material.REDSTONE);
    ItemMeta removeTempMeta = removeTempItem.getItemMeta();
    removeTempMeta.setDisplayName(LanguageManager.langMessage("gui.stats.remove_temp.title"));
    List<String> removeTempLore = new ArrayList<>();
    removeTempLore.add(LanguageManager.langMessage("gui.stats.remove_temp.lore"));
    removeTempMeta.setLore(removeTempLore);
    removeTempItem.setItemMeta(removeTempMeta);
    gui.setItem(REMOVE_TEMP_TABLE_SLOT, removeTempItem);

    player.openInventory(gui);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) return;
    Player player = (Player) event.getWhoClicked();

    if (event.getView().getTitle().equals(LanguageManager.langMessage("gui.stats.title"))) {
      event.setCancelled(true);

      ItemStack clicked = event.getCurrentItem();
      if (clicked == null || clicked.getType() == Material.AIR) return;

      switch (event.getSlot()) {
        case BACK_SLOT: // Back button
          mainGUI.openMainMenu(player);
          break;
        case DEFAULT_TABLE_SLOT: // Set default table
          player.closeInventory();
          player.sendMessage(LanguageManager.langMessage("gui.stats.default.input"));
          waitingForInput.put(player.getUniqueId(), "default");
          break;
        case ADD_TABLE_SLOT: // Add table
          player.closeInventory();
          player.sendMessage(LanguageManager.langMessage("gui.stats.add.input"));
          waitingForInput.put(player.getUniqueId(), "add");
          break;
        case REMOVE_TABLE_SLOT: // Remove table
          player.closeInventory();
          player.sendMessage(LanguageManager.langMessage("gui.stats.remove.input"));
          waitingForInput.put(player.getUniqueId(), "remove");
          break;
        case LIST_TABLES_SLOT: // List tables
          player.closeInventory();
          statsConfig.listTables(player);
          break;
        case ADD_MAP_TABLE_SLOT: // Add map table
          player.closeInventory();
          player.sendMessage(LanguageManager.langMessage("gui.stats.add_map.input"));
          waitingForInput.put(player.getUniqueId(), "addmap");
          break;
        case REMOVE_MAP_TABLE_SLOT: // Remove map table
          player.closeInventory();
          statsConfig.deleteTableForMap(player);
          break;
        case ADD_TEMP_TABLE_SLOT: // Add temporary table
          player.closeInventory();
          player.sendMessage(LanguageManager.langMessage("gui.stats.add_temp.input"));
          waitingForInput.put(player.getUniqueId(), "addtemp");
          break;
        case REMOVE_TEMP_TABLE_SLOT: // Remove temporary table
          player.closeInventory();
          statsConfig.removeTempTable(player);
          // Reopen menu after a small delay to avoid recursion
          Bukkit.getScheduler()
              .runTaskLater(
                  TowersForPGM.getInstance(),
                  () -> {
                    openStatsMenu(player);
                  },
                  1L);
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
      String action = waitingForInput.remove(playerId);
      String message = event.getMessage();

      Bukkit.getScheduler().runTask(TowersForPGM.getInstance(), () -> {
        switch (action) {
          case "default":
            statsConfig.setDefaultTable(player, message);
            break;
          case "add":
            statsConfig.addTable(player, message);
            break;
          case "remove":
            statsConfig.deleteTable(player, message);
            break;
          case "addmap":
            statsConfig.addTableForMap(player, message);
            break;
          case "addtemp":
            statsConfig.addTempTable(player, message);
            break;
        }
        // Reopen menu after a small delay to avoid recursion
        Bukkit.getScheduler()
            .runTaskLater(
                TowersForPGM.getInstance(),
                () -> {
                  openStatsMenu(player);
                },
                1L);
      });
    }
  }
}
