package org.nicolie.towersforpgm.gui;

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
import org.nicolie.towersforpgm.commandUtils.DraftConfig;
import org.nicolie.towersforpgm.commandUtils.PreparationConfig;
import org.nicolie.towersforpgm.commandUtils.RankedConfig;
import org.nicolie.towersforpgm.commandUtils.RefillConfig;
import org.nicolie.towersforpgm.commandUtils.StatsConfig;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.utils.LanguageManager;

import java.util.ArrayList;
import java.util.List;

public class TowersConfigGUI implements Listener {
    private static final int DRAFT_SLOT = 10;
    private static final int PREPARATION_SLOT = 11;
    private static final int RANKED_SLOT = 12;
    private static final int REFILL_SLOT = 13;
    private static final int STATS_SLOT = 14;
    private static boolean eventsRegistered = false;
    private final LanguageManager languageManager;
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

    public TowersConfigGUI(LanguageManager languageManager) {
        this.languageManager = languageManager;
        this.refillManager = TowersForPGM.getInstance().getRefillManager();
        this.draftConfig = new DraftConfig(languageManager);
        this.statsConfig = new StatsConfig(languageManager);
        this.preparationConfig = new PreparationConfig(languageManager);
        this.refillConfig = new RefillConfig(languageManager, refillManager);
        this.rankedConfig = new RankedConfig(languageManager);
        
        // Registrar eventos solo una vez para esta clase
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, TowersForPGM.getInstance());
            eventsRegistered = true;
        }
    }

    public void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, languageManager.getPluginMessage("gui.main.title"));
        
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
        draftMeta.setDisplayName(languageManager.getPluginMessage("gui.main.draft.title"));
        List<String> draftLore = new ArrayList<>();
        draftLore.add(languageManager.getPluginMessage("gui.main.draft.description"));
        draftLore.add(languageManager.getPluginMessage("gui.main.draft.feature1"));
        draftLore.add(languageManager.getPluginMessage("gui.main.draft.feature2"));
        draftLore.add(languageManager.getPluginMessage("gui.main.draft.feature3"));
        draftLore.add(languageManager.getPluginMessage("gui.main.draft.feature4"));
        draftLore.add(languageManager.getPluginMessage("gui.main.draft.feature5"));
        draftLore.add(languageManager.getPluginMessage("gui.main.draft.feature6"));
        draftMeta.setLore(draftLore);
        draftItem.setItemMeta(draftMeta);
        
        // Preparation Configuration
        ItemStack prepItem = new ItemStack(Material.WATCH);
        ItemMeta prepMeta = prepItem.getItemMeta();
        prepMeta.setDisplayName(languageManager.getPluginMessage("gui.main.preparation.title"));
        List<String> prepLore = new ArrayList<>();
        prepLore.add(languageManager.getPluginMessage("gui.main.preparation.description"));
        prepLore.add(languageManager.getPluginMessage("gui.main.preparation.feature1"));
        prepLore.add(languageManager.getPluginMessage("gui.main.preparation.feature2"));
        prepLore.add(languageManager.getPluginMessage("gui.main.preparation.feature3"));
        prepLore.add(languageManager.getPluginMessage("gui.main.preparation.feature4"));
        prepLore.add(languageManager.getPluginMessage("gui.main.preparation.feature5"));
        prepMeta.setLore(prepLore);
        prepItem.setItemMeta(prepMeta);
        
        // Refill Configuration
        ItemStack refillItem = new ItemStack(Material.CHEST);
        ItemMeta refillMeta = refillItem.getItemMeta();
        refillMeta.setDisplayName(languageManager.getPluginMessage("gui.main.refill.title"));
        List<String> refillLore = new ArrayList<>();
        refillLore.add(languageManager.getPluginMessage("gui.main.refill.description"));
        refillLore.add(languageManager.getPluginMessage("gui.main.refill.feature1"));
        refillLore.add(languageManager.getPluginMessage("gui.main.refill.feature2"));
        refillLore.add(languageManager.getPluginMessage("gui.main.refill.feature3"));
        refillMeta.setLore(refillLore);
        refillItem.setItemMeta(refillMeta);
        
        // Stats Configuration
        ItemStack statsItem = new ItemStack(Material.BOOK);
        ItemMeta statsMeta = statsItem.getItemMeta();
        statsMeta.setDisplayName(languageManager.getPluginMessage("gui.main.stats.title"));
        List<String> statsLore = new ArrayList<>();
        statsLore.add(languageManager.getPluginMessage("gui.main.stats.description"));
        statsLore.add(languageManager.getPluginMessage("gui.main.stats.feature1"));
        statsLore.add(languageManager.getPluginMessage("gui.main.stats.feature2"));
        statsLore.add(languageManager.getPluginMessage("gui.main.stats.feature3"));
        statsLore.add(languageManager.getPluginMessage("gui.main.stats.feature4"));
        statsMeta.setLore(statsLore);
        statsItem.setItemMeta(statsMeta);
        
        // Ranked Configuration
        ItemStack rankedItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta rankedMeta = rankedItem.getItemMeta();
        rankedMeta.setDisplayName(languageManager.getPluginMessage("gui.main.ranked.title"));
        List<String> rankedLore = new ArrayList<>();
        rankedLore.add(languageManager.getPluginMessage("gui.main.ranked.description"));
        rankedLore.add(languageManager.getPluginMessage("gui.main.ranked.feature1"));
        rankedLore.add(languageManager.getPluginMessage("gui.main.ranked.feature2"));
        rankedLore.add(languageManager.getPluginMessage("gui.main.ranked.feature3"));
        rankedLore.add(languageManager.getPluginMessage("gui.main.ranked.feature4"));
        rankedLore.add(languageManager.getPluginMessage("gui.main.ranked.feature5"));
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
        
        if (event.getView().getTitle().equals(languageManager.getPluginMessage("gui.main.title"))) {
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
            draftGUI = new DraftGUI(languageManager, draftConfig, this);
        }
        draftGUI.openDraftMenu(player);
    }

    private void openPreparationMenu(Player player) {
        if (preparationGUI == null) {
            preparationGUI = new PreparationGUI(languageManager, preparationConfig, this);
        }
        preparationGUI.openPreparationMenu(player);
    }

    private void openRefillMenu(Player player) {
        if (refillGUI == null) {
            refillGUI = new RefillGUI(languageManager, refillConfig, this);
        }
        refillGUI.openRefillMenu(player);
    }

    private void openRankedMenu(Player player) {
        if (rankedGUI == null) {
            rankedGUI = new RankedGUI(languageManager, rankedConfig, this);
        }
        rankedGUI.openRankedMenu(player);
    }

    private void openStatsMenu(Player player) {
        if (statsGUI == null) {
            statsGUI = new StatsGUI(languageManager, statsConfig, this);
        }
        statsGUI.openStatsMenu(player);
    }

    public void openMainMenu(Player player, String fromMenu) {
        // Method to return to main menu from submenus
        openMainMenu(player);
    }
}
