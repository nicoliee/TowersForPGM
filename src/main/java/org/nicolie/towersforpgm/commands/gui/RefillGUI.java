package org.nicolie.towersforpgm.commands.gui;

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
import org.nicolie.towersforpgm.commands.commandUtils.RefillConfig;
import org.nicolie.towersforpgm.utils.LanguageManager;
import tc.oc.pgm.api.PGM;

public class RefillGUI implements Listener {
  private static final int BACK_SLOT = 0;
  private static final int ADD_SLOT = 11;
  private static final int REMOVE_SLOT = 13;
  private static final int RELOAD_SLOT = 15;

  private static boolean eventsRegistered = false;
  private final RefillConfig refillConfig;
  private final TowersConfigGUI mainGUI;

  public RefillGUI(RefillConfig refillConfig, TowersConfigGUI mainGUI) {
    this.refillConfig = refillConfig;
    this.mainGUI = mainGUI;

    // Registrar eventos solo una vez para esta clase
    if (!eventsRegistered) {
      Bukkit.getPluginManager().registerEvents(this, TowersForPGM.getInstance());
      eventsRegistered = true;
    }
  }

  public void openRefillMenu(Player player) {
    Inventory gui =
        Bukkit.createInventory(null, 27, LanguageManager.langMessage("gui.refill.title"));

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

    // ===== REFILL ACTIONS =====
    // Add refill location
    ItemStack addItem = new ItemStack(Material.CHEST);
    ItemMeta addMeta = addItem.getItemMeta();
    addMeta.setDisplayName(LanguageManager.langMessage("gui.refill.add.title"));
    List<String> addLore = new ArrayList<>();
    addLore.add(LanguageManager.langMessage("gui.refill.add.lore1"));
    addLore.add(LanguageManager.langMessage("gui.refill.add.lore2"));
    addMeta.setLore(addLore);
    addItem.setItemMeta(addMeta);
    gui.setItem(ADD_SLOT, addItem);

    // Remove refill location
    ItemStack removeItem = new ItemStack(Material.BARRIER);
    ItemMeta removeMeta = removeItem.getItemMeta();
    removeMeta.setDisplayName(LanguageManager.langMessage("gui.refill.remove.title"));
    List<String> removeLore = new ArrayList<>();
    removeLore.add(LanguageManager.langMessage("gui.refill.remove.lore1"));
    removeLore.add(LanguageManager.langMessage("gui.refill.remove.lore2"));
    removeMeta.setLore(removeLore);
    removeItem.setItemMeta(removeMeta);
    gui.setItem(REMOVE_SLOT, removeItem);

    // Test/Reload refill
    ItemStack reloadItem = new ItemStack(Material.REDSTONE);
    ItemMeta reloadMeta = reloadItem.getItemMeta();
    reloadMeta.setDisplayName(LanguageManager.langMessage("gui.refill.reload.title"));
    List<String> reloadLore = new ArrayList<>();
    reloadLore.add(LanguageManager.langMessage("gui.refill.reload.lore1"));
    reloadLore.add(LanguageManager.langMessage("gui.refill.reload.lore2"));
    reloadMeta.setLore(reloadLore);
    reloadItem.setItemMeta(reloadMeta);
    gui.setItem(RELOAD_SLOT, reloadItem);

    player.openInventory(gui);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) return;
    Player player = (Player) event.getWhoClicked();

    if (event.getView().getTitle().equals(LanguageManager.langMessage("gui.refill.title"))) {
      event.setCancelled(true);

      ItemStack clicked = event.getCurrentItem();
      if (clicked == null || clicked.getType() == Material.AIR) return;

      switch (event.getSlot()) {
        case BACK_SLOT:
          mainGUI.openMainMenu(player);
          break;

        case ADD_SLOT:
          player.closeInventory();
          String addMapName =
              PGM.get().getMatchManager().getMatch(player).getMap().getName();
          refillConfig.addRefillLocation(player, addMapName, player.getLocation());
          break;

        case REMOVE_SLOT:
          player.closeInventory();
          String removeMapName =
              PGM.get().getMatchManager().getMatch(player).getMap().getName();
          refillConfig.removeRefillLocation(player, removeMapName, player.getLocation());
          break;

        case RELOAD_SLOT:
          player.closeInventory();
          String reloadMapName =
              PGM.get().getMatchManager().getMatch(player).getMap().getName();
          String worldName = player.getWorld().getName();
          refillConfig.testRefill(player, reloadMapName, worldName);
          break;
      }
    }
  }
}
