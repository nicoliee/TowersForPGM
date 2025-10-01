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

public class PreparationGUI implements Listener {
  private static final int BACK_SLOT = 0;
  private static final int TOGGLE_SLOT = 10;
  private static final int ADD_SLOT = 11;
  private static final int REMOVE_SLOT = 12;
  private static final int MAX_SLOT = 13;
  private static final int MIN_SLOT = 14;
  private static final int TIMER_SLOT = 15;
  private static final int HASTE_SLOT = 16;
  private static final int LIST_SLOT = 22;

  private static boolean eventsRegistered = false;
  private final LanguageManager languageManager;
  private final PreparationConfig preparationConfig;
  private final TowersConfigGUI mainGUI;

  public PreparationGUI(
      LanguageManager languageManager,
      PreparationConfig preparationConfig,
      TowersConfigGUI mainGUI) {
    this.languageManager = languageManager;
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
        Bukkit.createInventory(null, 45, languageManager.getPluginMessage("gui.preparation.title"));

    // Gray glass border
    ItemStack grayGlass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
    ItemMeta grayMeta = grayGlass.getItemMeta();
    grayMeta.setDisplayName(" ");
    grayGlass.setItemMeta(grayMeta);

    // Fill border with gray glass
    for (int i = 0; i < 45; i++) {
      if (i < 9 || i > 35 || i % 9 == 0 || i % 9 == 8) {
        gui.setItem(i, grayGlass);
      }
    }

    // Back button (red glass)
    ItemStack backItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
    ItemMeta backMeta = backItem.getItemMeta();
    backMeta.setDisplayName(languageManager.getPluginMessage("gui.back"));
    backItem.setItemMeta(backMeta);
    gui.setItem(BACK_SLOT, backItem);

    // Toggle preparation
    ItemStack toggleItem = new ItemStack(Material.REDSTONE_TORCH_ON);
    ItemMeta toggleMeta = toggleItem.getItemMeta();
    toggleMeta.setDisplayName(languageManager.getPluginMessage("gui.preparation.toggle.title"));
    List<String> toggleLore = new ArrayList<>();
    toggleLore.add(languageManager.getPluginMessage("gui.preparation.toggle.lore1"));
    toggleLore.add(languageManager.getPluginMessage("gui.preparation.toggle.lore2"));
    toggleMeta.setLore(toggleLore);
    toggleItem.setItemMeta(toggleMeta);
    gui.setItem(TOGGLE_SLOT, toggleItem);

    // Add region
    ItemStack addItem = new ItemStack(Material.EMERALD_BLOCK);
    ItemMeta addMeta = addItem.getItemMeta();
    addMeta.setDisplayName(languageManager.getPluginMessage("gui.preparation.add.title"));
    List<String> addLore = new ArrayList<>();
    addLore.add(languageManager.getPluginMessage("gui.preparation.add.lore1"));
    addLore.add(languageManager.getPluginMessage("gui.preparation.add.lore2"));
    addMeta.setLore(addLore);
    addItem.setItemMeta(addMeta);
    gui.setItem(ADD_SLOT, addItem);

    // Remove region
    ItemStack removeItem = new ItemStack(Material.REDSTONE_BLOCK);
    ItemMeta removeMeta = removeItem.getItemMeta();
    removeMeta.setDisplayName(languageManager.getPluginMessage("gui.preparation.remove.title"));
    List<String> removeLore = new ArrayList<>();
    removeLore.add(languageManager.getPluginMessage("gui.preparation.remove.lore1"));
    removeLore.add(languageManager.getPluginMessage("gui.preparation.remove.lore2"));
    removeMeta.setLore(removeLore);
    removeItem.setItemMeta(removeMeta);
    gui.setItem(REMOVE_SLOT, removeItem);

    // Set max coordinates
    ItemStack maxItem = new ItemStack(Material.DIAMOND_BLOCK);
    ItemMeta maxMeta = maxItem.getItemMeta();
    maxMeta.setDisplayName(languageManager.getPluginMessage("gui.preparation.max.title"));
    List<String> maxLore = new ArrayList<>();
    maxLore.add(languageManager.getPluginMessage("gui.preparation.max.lore1"));
    maxLore.add(languageManager.getPluginMessage("gui.preparation.max.lore2"));
    maxLore.add(languageManager.getPluginMessage("gui.preparation.max.lore3"));
    maxMeta.setLore(maxLore);
    maxItem.setItemMeta(maxMeta);
    gui.setItem(MAX_SLOT, maxItem);

    // Set min coordinates
    ItemStack minItem = new ItemStack(Material.IRON_BLOCK);
    ItemMeta minMeta = minItem.getItemMeta();
    minMeta.setDisplayName(languageManager.getPluginMessage("gui.preparation.min.title"));
    List<String> minLore = new ArrayList<>();
    minLore.add(languageManager.getPluginMessage("gui.preparation.min.lore1"));
    minLore.add(languageManager.getPluginMessage("gui.preparation.min.lore2"));
    minLore.add(languageManager.getPluginMessage("gui.preparation.min.lore3"));
    minMeta.setLore(minLore);
    minItem.setItemMeta(minMeta);
    gui.setItem(MIN_SLOT, minItem);

    // Timer settings
    ItemStack timerItem = new ItemStack(Material.WATCH);
    ItemMeta timerMeta = timerItem.getItemMeta();
    timerMeta.setDisplayName(languageManager.getPluginMessage("gui.preparation.timer.title"));
    List<String> timerLore = new ArrayList<>();
    timerLore.add(languageManager.getPluginMessage("gui.preparation.timer.lore1"));
    timerLore.add(languageManager.getPluginMessage("gui.preparation.timer.lore2"));
    timerMeta.setLore(timerLore);
    timerItem.setItemMeta(timerMeta);
    gui.setItem(TIMER_SLOT, timerItem);

    // Haste settings
    ItemStack hasteItem = new ItemStack(Material.SUGAR);
    ItemMeta hasteMeta = hasteItem.getItemMeta();
    hasteMeta.setDisplayName(languageManager.getPluginMessage("gui.preparation.haste.title"));
    List<String> hasteLore = new ArrayList<>();
    hasteLore.add(languageManager.getPluginMessage("gui.preparation.haste.lore1"));
    hasteLore.add(languageManager.getPluginMessage("gui.preparation.haste.lore2"));
    hasteMeta.setLore(hasteLore);
    hasteItem.setItemMeta(hasteMeta);
    gui.setItem(HASTE_SLOT, hasteItem);

    // List regions
    ItemStack listItem = new ItemStack(Material.BOOK);
    ItemMeta listMeta = listItem.getItemMeta();
    listMeta.setDisplayName(languageManager.getPluginMessage("gui.preparation.list.title"));
    List<String> listLore = new ArrayList<>();
    listLore.add(languageManager.getPluginMessage("gui.preparation.list.lore1"));
    listLore.add(languageManager.getPluginMessage("gui.preparation.list.lore2"));
    listMeta.setLore(listLore);
    listItem.setItemMeta(listMeta);
    gui.setItem(LIST_SLOT, listItem);

    player.openInventory(gui);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) return;
    Player player = (Player) event.getWhoClicked();

    if (event
        .getView()
        .getTitle()
        .equals(languageManager.getPluginMessage("gui.preparation.title"))) {
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
          player.sendMessage(languageManager.getPluginMessage("gui.preparation.max.command1"));
          player.sendMessage(languageManager.getPluginMessage("gui.preparation.max.command2"));
          break;
        case MIN_SLOT: // Set min coordinates
          player.closeInventory();
          player.sendMessage(languageManager.getPluginMessage("gui.preparation.min.command1"));
          player.sendMessage(languageManager.getPluginMessage("gui.preparation.min.command2"));
          break;
        case TIMER_SLOT: // Timer settings
          player.closeInventory();
          player.sendMessage(languageManager.getPluginMessage("gui.preparation.timer.command1"));
          player.sendMessage(languageManager.getPluginMessage("gui.preparation.timer.command2"));
          break;
        case HASTE_SLOT: // Haste settings
          player.closeInventory();
          player.sendMessage(languageManager.getPluginMessage("gui.preparation.haste.command1"));
          player.sendMessage(languageManager.getPluginMessage("gui.preparation.haste.command2"));
          break;
        case LIST_SLOT: // List regions
          player.closeInventory();
          preparationConfig.handleListCommand(player);
          break;
      }
    }
  }
}
