package org.nicolie.towersforpgm.gui.towersCommand;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.commandUtils.PreparationConfig;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;

public class PreparationGUI implements Listener {
  private static final int BACK_SLOT = 0;
  private static final int TOGGLE_SLOT = 9;
  private static final int MIN_SLOT = 10;
  private static final int MAX_SLOT = 11;
  private static final int TIMER_SLOT = 12;
  private static final int HASTE_SLOT = 14;
  private static final int ADD_SLOT = 15;
  private static final int REMOVE_SLOT = 16;
  private static final int LIST_SLOT = 17;

  private static boolean eventsRegistered = false;
  private final PreparationConfig preparationConfig;
  private final TowersConfigGUI mainGUI;

  public PreparationGUI(PreparationConfig preparationConfig, TowersConfigGUI mainGUI) {
    this.preparationConfig = preparationConfig;
    this.mainGUI = mainGUI;

    // Registrar eventos solo una vez para esta clase
    if (!eventsRegistered) {
      Bukkit.getPluginManager().registerEvents(this, TowersForPGM.getInstance());
      eventsRegistered = true;
    }
  }

  public void openPreparationMenu(Player player) {
    Inventory gui =
        Bukkit.createInventory(null, 27, LanguageManager.langMessage("gui.preparation.title"));

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

    // Get current map name
    String mapName = PGM.get().getMatchManager().getMatch(player).getMap().getName();
    TowersForPGM plugin = TowersForPGM.getInstance();
    boolean preparationEnabled = plugin.isPreparationEnabled();

    // ===== PREPARATION CONTROLS =====
    // Toggle preparation
    ItemStack toggleItem = new ItemStack(preparationEnabled ? Material.EMERALD : Material.REDSTONE);
    ItemMeta toggleMeta = toggleItem.getItemMeta();
    toggleMeta.setDisplayName(LanguageManager.langMessage("gui.preparation.toggle.title"));
    List<String> toggleLore = new ArrayList<>();
    toggleLore.add(LanguageManager.langMessage("gui.preparation.toggle.current")
        .replace(
            "{status}",
            preparationEnabled
                ? LanguageManager.langMessage("gui.status.enabled")
                : LanguageManager.langMessage("gui.status.disabled")));
    toggleLore.add("§7");
    toggleLore.add(LanguageManager.langMessage("gui.preparation.toggle.lore1"));
    toggleLore.add(LanguageManager.langMessage("gui.preparation.toggle.lore2"));
    toggleMeta.setLore(toggleLore);
    toggleItem.setItemMeta(toggleMeta);
    gui.setItem(TOGGLE_SLOT, toggleItem);

    // Add region
    ItemStack addItem = new ItemStack(Material.EMERALD_BLOCK);
    ItemMeta addMeta = addItem.getItemMeta();
    addMeta.setDisplayName(LanguageManager.langMessage("gui.preparation.add.title"));
    List<String> addLore = new ArrayList<>();
    addLore.add(LanguageManager.langMessage("gui.preparation.add.lore1"));
    addLore.add(LanguageManager.langMessage("gui.preparation.add.lore2"));
    addMeta.setLore(addLore);
    addItem.setItemMeta(addMeta);
    gui.setItem(ADD_SLOT, addItem);

    // Remove region
    ItemStack removeItem = new ItemStack(Material.REDSTONE_BLOCK);
    ItemMeta removeMeta = removeItem.getItemMeta();
    removeMeta.setDisplayName(LanguageManager.langMessage("gui.preparation.remove.title"));
    List<String> removeLore = new ArrayList<>();
    removeLore.add(LanguageManager.langMessage("gui.preparation.remove.lore1"));
    removeLore.add(LanguageManager.langMessage("gui.preparation.remove.lore2"));
    removeMeta.setLore(removeLore);
    removeItem.setItemMeta(removeMeta);
    gui.setItem(REMOVE_SLOT, removeItem);

    // List regions
    ItemStack listItem = new ItemStack(Material.BOOK);
    ItemMeta listMeta = listItem.getItemMeta();
    listMeta.setDisplayName(LanguageManager.langMessage("gui.preparation.list.title"));
    List<String> listLore = new ArrayList<>();
    listLore.add(LanguageManager.langMessage("gui.preparation.list.lore1"));
    listLore.add(LanguageManager.langMessage("gui.preparation.list.lore2"));
    listMeta.setLore(listLore);
    listItem.setItemMeta(listMeta);
    gui.setItem(LIST_SLOT, listItem);

    // Get current coordinates for this map
    String basePath = "preparationTime.maps." + mapName;
    String maxCoords = plugin.getConfig().getString(basePath + ".P2", "");
    String minCoords = plugin.getConfig().getString(basePath + ".P1", "");
    int timer = plugin.getConfig().getInt(basePath + ".timer", 0);
    int haste = plugin.getConfig().getInt(basePath + ".haste", 0);

    // Set max coordinates
    ItemStack maxItem = new ItemStack(Material.DIAMOND_BLOCK);
    ItemMeta maxMeta = maxItem.getItemMeta();
    maxMeta.setDisplayName(LanguageManager.langMessage("gui.preparation.max.title"));
    List<String> maxLore = new ArrayList<>();
    maxLore.add(
        LanguageManager.langMessage("gui.preparation.max.current").replace("{coords}", maxCoords));
    maxLore.add("§7");
    maxLore.add(LanguageManager.langMessage("gui.preparation.max.lore1"));
    maxLore.add(LanguageManager.langMessage("gui.preparation.max.lore2"));
    maxLore.add(LanguageManager.langMessage("gui.preparation.max.lore3"));
    maxMeta.setLore(maxLore);
    maxItem.setItemMeta(maxMeta);
    gui.setItem(MAX_SLOT, maxItem);

    // Set min coordinates
    ItemStack minItem = new ItemStack(Material.IRON_BLOCK);
    ItemMeta minMeta = minItem.getItemMeta();
    minMeta.setDisplayName(LanguageManager.langMessage("gui.preparation.min.title"));
    List<String> minLore = new ArrayList<>();
    minLore.add(
        LanguageManager.langMessage("gui.preparation.min.current").replace("{coords}", minCoords));
    minLore.add("§7");
    minLore.add(LanguageManager.langMessage("gui.preparation.min.lore1"));
    minLore.add(LanguageManager.langMessage("gui.preparation.min.lore2"));
    minLore.add(LanguageManager.langMessage("gui.preparation.min.lore3"));
    minMeta.setLore(minLore);
    minItem.setItemMeta(minMeta);
    gui.setItem(MIN_SLOT, minItem);

    // Timer settings
    ItemStack timerItem = new ItemStack(Material.WATCH);
    ItemMeta timerMeta = timerItem.getItemMeta();
    timerMeta.setDisplayName(LanguageManager.langMessage("gui.preparation.timer.title"));
    List<String> timerLore = new ArrayList<>();
    timerLore.add(LanguageManager.langMessage("gui.preparation.timer.current")
        .replace("{timer}", timer > 0 ? timer + " min" : ""));
    timerLore.add("§7");
    timerLore.add(LanguageManager.langMessage("gui.preparation.timer.lore1"));
    timerLore.add(LanguageManager.langMessage("gui.preparation.timer.lore2"));
    timerMeta.setLore(timerLore);
    timerItem.setItemMeta(timerMeta);
    gui.setItem(TIMER_SLOT, timerItem);

    // Haste settings
    ItemStack hasteItem = new ItemStack(Material.SUGAR);
    ItemMeta hasteMeta = hasteItem.getItemMeta();
    hasteMeta.setDisplayName(LanguageManager.langMessage("gui.preparation.haste.title"));
    List<String> hasteLore = new ArrayList<>();
    hasteLore.add(LanguageManager.langMessage("gui.preparation.haste.current")
        .replace("{haste}", haste > 0 ? haste + " min" : ""));
    hasteLore.add("§7");
    hasteLore.add(LanguageManager.langMessage("gui.preparation.haste.lore1"));
    hasteLore.add(LanguageManager.langMessage("gui.preparation.haste.lore2"));
    hasteMeta.setLore(hasteLore);
    hasteItem.setItemMeta(hasteMeta);
    gui.setItem(HASTE_SLOT, hasteItem);

    player.openInventory(gui);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) return;
    Player player = (Player) event.getWhoClicked();

    if (event.getView().getTitle().equals(LanguageManager.langMessage("gui.preparation.title"))) {
      event.setCancelled(true);

      ItemStack clicked = event.getCurrentItem();
      if (clicked == null || clicked.getType() == Material.AIR) return;

      switch (event.getSlot()) {
        case BACK_SLOT: // Back button
          mainGUI.openMainMenu(player);
          break;
        case TOGGLE_SLOT: // Toggle preparation
          player.closeInventory();
          preparationConfig.togglePreparation(player);
          break;
        case ADD_SLOT: // Add region
          player.closeInventory();
          preparationConfig.handleAddCommand(player);
          break;
        case REMOVE_SLOT: // Remove region
          player.closeInventory();
          preparationConfig.handleRemoveCommand(player);
          break;
        case MAX_SLOT: // Set max coordinates
          player.closeInventory();
          player.sendMessage(LanguageManager.langMessage("gui.preparation.max.command1"));
          player.sendMessage(LanguageManager.langMessage("gui.preparation.max.command2"));
          break;
        case MIN_SLOT: // Set min coordinates
          player.closeInventory();
          player.sendMessage(LanguageManager.langMessage("gui.preparation.min.command1"));
          player.sendMessage(LanguageManager.langMessage("gui.preparation.min.command2"));
          break;
        case TIMER_SLOT: // Timer settings
          player.closeInventory();
          player.sendMessage(LanguageManager.langMessage("gui.preparation.timer.command1"));
          player.sendMessage(LanguageManager.langMessage("gui.preparation.timer.command2"));
          break;
        case HASTE_SLOT: // Haste settings
          player.closeInventory();
          player.sendMessage(LanguageManager.langMessage("gui.preparation.haste.command1"));
          player.sendMessage(LanguageManager.langMessage("gui.preparation.haste.command2"));
          break;
        case LIST_SLOT: // List regions
          player.closeInventory();
          preparationConfig.handleListCommand(player);
          break;
      }
    }
  }
}
