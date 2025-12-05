package org.nicolie.towersforpgm.commands.gui;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.commands.commandUtils.DraftConfig;
import org.nicolie.towersforpgm.commands.commandUtils.PreparationConfig;
import org.nicolie.towersforpgm.commands.commandUtils.RankedConfig;
import org.nicolie.towersforpgm.commands.commandUtils.RefillConfig;
import org.nicolie.towersforpgm.commands.commandUtils.StatsConfig;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.utils.LanguageManager;

public class TowersConfigGUI implements Listener {
  private static final int DRAFT_SLOT = 11;
  private static final int PREPARATION_SLOT = 12;
  private static final int RANKED_SLOT = 13;
  private static final int REFILL_SLOT = 14;
  private static final int STATS_SLOT = 15;
  private static boolean eventsRegistered = false;
  private final DraftConfig draftConfig;
  private final StatsConfig statsConfig;
  private final PreparationConfig preparationConfig;
  private final RefillConfig refillConfig;
  private final RankedConfig rankedConfig;
  private final RefillManager refillManager;

  private StatsGUI statsGUI;
  private DraftGUI draftGUI;
  private PreparationGUI preparationGUI;
  private RefillGUI refillGUI;
  private RankedGUI rankedGUI;

  public TowersConfigGUI() {
    this.refillManager = TowersForPGM.getInstance().getRefillManager();
    this.draftConfig = new DraftConfig();
    this.statsConfig = new StatsConfig();
    this.preparationConfig = new PreparationConfig();
    this.refillConfig = new RefillConfig(refillManager);
    this.rankedConfig = new RankedConfig();

    // Registrar eventos solo una vez para esta clase
    if (!eventsRegistered) {
      Bukkit.getPluginManager().registerEvents(this, TowersForPGM.getInstance());
      eventsRegistered = true;
    }
  }

  public void openMainMenu(Player player) {
    Inventory gui = Bukkit.createInventory(null, 27, LanguageManager.message("gui.main.title"));

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

    // Draft Configuration
    ItemStack draftItem = new ItemStack(Material.PAPER);
    ItemMeta draftMeta = draftItem.getItemMeta();
    draftMeta.setDisplayName(LanguageManager.message("gui.main.draft.title"));
    List<String> draftLore = new ArrayList<>();
    draftLore.add(LanguageManager.message("gui.main.draft.description"));
    draftLore.add(LanguageManager.message("gui.main.draft.feature1"));
    draftLore.add(LanguageManager.message("gui.main.draft.feature2"));
    draftLore.add(LanguageManager.message("gui.main.draft.feature3"));
    draftLore.add(LanguageManager.message("gui.main.draft.feature4"));
    draftLore.add(LanguageManager.message("gui.main.draft.feature5"));
    draftLore.add(LanguageManager.message("gui.main.draft.feature6"));
    draftMeta.setLore(draftLore);
    draftItem.setItemMeta(draftMeta);

    // Preparation Configuration
    ItemStack prepItem = new ItemStack(Material.WATCH);
    ItemMeta prepMeta = prepItem.getItemMeta();
    prepMeta.setDisplayName(LanguageManager.message("gui.main.preparation.title"));
    List<String> prepLore = new ArrayList<>();
    prepLore.add(LanguageManager.message("gui.main.preparation.description"));
    prepLore.add(LanguageManager.message("gui.main.preparation.feature1"));
    prepLore.add(LanguageManager.message("gui.main.preparation.feature2"));
    prepLore.add(LanguageManager.message("gui.main.preparation.feature3"));
    prepLore.add(LanguageManager.message("gui.main.preparation.feature4"));
    prepLore.add(LanguageManager.message("gui.main.preparation.feature5"));
    prepMeta.setLore(prepLore);
    prepItem.setItemMeta(prepMeta);

    // Refill Configuration
    ItemStack refillItem = new ItemStack(Material.CHEST);
    ItemMeta refillMeta = refillItem.getItemMeta();
    refillMeta.setDisplayName(LanguageManager.message("gui.main.refill.title"));
    List<String> refillLore = new ArrayList<>();
    refillLore.add(LanguageManager.message("gui.main.refill.description"));
    refillLore.add(LanguageManager.message("gui.main.refill.feature1"));
    refillLore.add(LanguageManager.message("gui.main.refill.feature2"));
    refillLore.add(LanguageManager.message("gui.main.refill.feature3"));
    refillMeta.setLore(refillLore);
    refillItem.setItemMeta(refillMeta);

    // Stats Configuration
    ItemStack statsItem = new ItemStack(Material.BOOK);
    ItemMeta statsMeta = statsItem.getItemMeta();
    statsMeta.setDisplayName(LanguageManager.message("gui.main.stats.title"));
    List<String> statsLore = new ArrayList<>();
    statsLore.add(LanguageManager.message("gui.main.stats.description"));
    statsLore.add(LanguageManager.message("gui.main.stats.feature1"));
    statsLore.add(LanguageManager.message("gui.main.stats.feature2"));
    statsLore.add(LanguageManager.message("gui.main.stats.feature3"));
    statsLore.add(LanguageManager.message("gui.main.stats.feature4"));
    statsMeta.setLore(statsLore);
    statsItem.setItemMeta(statsMeta);

    // Ranked Configuration
    ItemStack rankedItem = new ItemStack(Material.NETHER_STAR);
    ItemMeta rankedMeta = rankedItem.getItemMeta();
    rankedMeta.setDisplayName(LanguageManager.message("gui.main.ranked.title"));
    List<String> rankedLore = new ArrayList<>();
    rankedLore.add(LanguageManager.message("gui.main.ranked.description"));
    rankedLore.add(LanguageManager.message("gui.main.ranked.feature1"));
    rankedLore.add(LanguageManager.message("gui.main.ranked.feature2"));
    rankedLore.add(LanguageManager.message("gui.main.ranked.feature3"));
    rankedLore.add(LanguageManager.message("gui.main.ranked.feature4"));
    rankedLore.add(LanguageManager.message("gui.main.ranked.feature5"));
    rankedMeta.setLore(rankedLore);
    rankedItem.setItemMeta(rankedMeta);

    gui.setItem(DRAFT_SLOT, draftItem);
    gui.setItem(PREPARATION_SLOT, prepItem);
    gui.setItem(RANKED_SLOT, rankedItem);
    gui.setItem(REFILL_SLOT, refillItem);
    gui.setItem(STATS_SLOT, statsItem);

    player.openInventory(gui);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) return;
    Player player = (Player) event.getWhoClicked();

    if (event.getView().getTitle().equals(LanguageManager.message("gui.main.title"))) {
      event.setCancelled(true);

      ItemStack clicked = event.getCurrentItem();
      if (clicked == null || clicked.getType() == Material.AIR) return;

      switch (event.getSlot()) {
        case DRAFT_SLOT: // Draft Configuration
          openDraftMenu(player);
          break;
        case PREPARATION_SLOT: // Preparation Configuration
          openPreparationMenu(player);
          break;
        case RANKED_SLOT: // Ranked Configuration
          openRankedMenu(player);
          break;
        case REFILL_SLOT: // Refill Configuration
          openRefillMenu(player);
          break;
        case STATS_SLOT: // Stats Configuration
          openStatsMenu(player);
          break;
      }
    }
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    // Handle any cleanup if needed
  }

  private void openDraftMenu(Player player) {
    if (draftGUI == null) {
      draftGUI = new DraftGUI(draftConfig, this);
    }
    draftGUI.openDraftMenu(player);
  }

  private void openPreparationMenu(Player player) {
    if (preparationGUI == null) {
      preparationGUI = new PreparationGUI(preparationConfig, this);
    }
    preparationGUI.openPreparationMenu(player);
  }

  private void openRefillMenu(Player player) {
    if (refillGUI == null) {
      refillGUI = new RefillGUI(refillConfig, this);
    }
    refillGUI.openRefillMenu(player);
  }

  private void openRankedMenu(Player player) {
    if (rankedGUI == null) {
      rankedGUI = new RankedGUI(rankedConfig, this);
    }
    rankedGUI.openRankedMenu(player);
  }

  private void openStatsMenu(Player player) {
    if (statsGUI == null) {
      statsGUI = new StatsGUI(statsConfig, this);
    }
    statsGUI.openStatsMenu(player);
  }

  public void openMainMenu(Player player, String fromMenu) {
    // Method to return to main menu from submenus
    openMainMenu(player);
  }
}
