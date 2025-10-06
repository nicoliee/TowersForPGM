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
import org.nicolie.towersforpgm.commandUtils.DraftConfig;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;

public class DraftGUI implements Listener {
  private static final int BACK_SLOT = 0;
  private static final int MIN_PLAYERS_SLOT = 10;
  private static final int DRAFT_ORDER_SLOT = 11;
  private static final int SECOND_PICK_BALANCE_SLOT = 12;
  private static final int SUGGESTIONS_SLOT = 13;
  private static final int TIMER_SLOT = 14;
  private static final int PRIVATE_MATCHES_SLOT = 15;

  private static boolean eventsRegistered = false;
  private final DraftConfig draftConfig;
  private final TowersConfigGUI mainGUI;
  private final Map<UUID, String> waitingForInput = new HashMap<>();

  public DraftGUI(DraftConfig draftConfig, TowersConfigGUI mainGUI) {
    this.draftConfig = draftConfig;
    this.mainGUI = mainGUI;

    // Registrar eventos solo una vez para esta clase
    if (!eventsRegistered) {
      Bukkit.getPluginManager().registerEvents(this, TowersForPGM.getInstance());
      eventsRegistered = true;
    }
  }

  public void openDraftMenu(Player player) {
    Inventory gui =
        Bukkit.createInventory(null, 27, LanguageManager.langMessage("gui.draft.title"));

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

    // ===== BASIC CONFIGURATION =====
    // Min players
    ItemStack minItem = new ItemStack(Material.GOLD_NUGGET);
    ItemMeta minMeta = minItem.getItemMeta();
    minMeta.setDisplayName(LanguageManager.langMessage("gui.draft.min_players.title"));
    List<String> minLore = new ArrayList<>();
    minLore.add(LanguageManager.langMessage("gui.draft.min_players.current")
        .replace("{value}", String.valueOf(ConfigManager.getMinDraftOrder())));
    minLore.add("§7");
    minLore.add(LanguageManager.langMessage("gui.draft.min_players.lore1"));
    minLore.add(LanguageManager.langMessage("gui.draft.min_players.lore2"));
    minMeta.setLore(minLore);
    minItem.setItemMeta(minMeta);
    gui.setItem(MIN_PLAYERS_SLOT, minItem);

    // Draft order
    ItemStack orderItem = new ItemStack(Material.PAPER);
    ItemMeta orderMeta = orderItem.getItemMeta();
    orderMeta.setDisplayName(LanguageManager.langMessage("gui.draft.order.title"));
    List<String> orderLore = new ArrayList<>();
    orderLore.add(LanguageManager.langMessage("gui.draft.order.current")
        .replace("{value}", ConfigManager.getDraftOrder()));
    orderLore.add("§7");
    orderLore.add(LanguageManager.langMessage("gui.draft.order.lore1"));
    orderLore.add(LanguageManager.langMessage("gui.draft.order.lore2"));
    orderMeta.setLore(orderLore);
    orderItem.setItemMeta(orderMeta);
    gui.setItem(DRAFT_ORDER_SLOT, orderItem);

    // ===== TOGGLE OPTIONS =====
    // Second pick balance
    ItemStack secondPickItem = new ItemStack(Material.GOLD_INGOT);
    ItemMeta secondPickMeta = secondPickItem.getItemMeta();
    secondPickMeta.setDisplayName(LanguageManager.langMessage("gui.draft.second_pick.title"));
    List<String> secondPickLore = new ArrayList<>();
    boolean secondPickEnabled = ConfigManager.isSecondPickBalance();
    secondPickLore.add(LanguageManager.langMessage("gui.draft.second_pick.current")
        .replace(
            "{status}",
            secondPickEnabled
                ? LanguageManager.langMessage("gui.status.enabled")
                : LanguageManager.langMessage("gui.status.disabled")));
    secondPickLore.add("§7");
    secondPickLore.add(LanguageManager.langMessage("gui.draft.second_pick.lore1"));
    secondPickLore.add(LanguageManager.langMessage("gui.draft.second_pick.lore2"));
    secondPickMeta.setLore(secondPickLore);
    secondPickItem.setItemMeta(secondPickMeta);
    gui.setItem(SECOND_PICK_BALANCE_SLOT, secondPickItem);

    // Suggestions
    ItemStack suggestionsItem = new ItemStack(Material.EMERALD);
    ItemMeta suggestionsMeta = suggestionsItem.getItemMeta();
    suggestionsMeta.setDisplayName(LanguageManager.langMessage("gui.draft.suggestions.title"));
    List<String> suggestionsLore = new ArrayList<>();
    boolean suggestionsEnabled = ConfigManager.isDraftSuggestions();
    suggestionsLore.add(LanguageManager.langMessage("gui.draft.suggestions.current")
        .replace(
            "{status}",
            suggestionsEnabled
                ? LanguageManager.langMessage("gui.status.enabled")
                : LanguageManager.langMessage("gui.status.disabled")));
    suggestionsLore.add("§7");
    suggestionsLore.add(LanguageManager.langMessage("gui.draft.suggestions.lore1"));
    suggestionsLore.add(LanguageManager.langMessage("gui.draft.suggestions.lore2"));
    suggestionsMeta.setLore(suggestionsLore);
    suggestionsItem.setItemMeta(suggestionsMeta);
    gui.setItem(SUGGESTIONS_SLOT, suggestionsItem);

    // Timer
    ItemStack timerItem = new ItemStack(Material.WATCH);
    ItemMeta timerMeta = timerItem.getItemMeta();
    timerMeta.setDisplayName(LanguageManager.langMessage("gui.draft.timer.title"));
    List<String> timerLore = new ArrayList<>();
    boolean timerEnabled = ConfigManager.isDraftTimer();
    timerLore.add(LanguageManager.langMessage("gui.draft.timer.current")
        .replace(
            "{status}",
            timerEnabled
                ? LanguageManager.langMessage("gui.status.enabled")
                : LanguageManager.langMessage("gui.status.disabled")));
    timerLore.add("§7");
    timerLore.add(LanguageManager.langMessage("gui.draft.timer.lore1"));
    timerLore.add(LanguageManager.langMessage("gui.draft.timer.lore2"));
    timerMeta.setLore(timerLore);
    timerItem.setItemMeta(timerMeta);
    gui.setItem(TIMER_SLOT, timerItem);

    // Private matches (map-specific, shown as info only)
    ItemStack privateItem = new ItemStack(Material.REDSTONE_TORCH_ON);
    ItemMeta privateMeta = privateItem.getItemMeta();
    privateMeta.setDisplayName(LanguageManager.langMessage("gui.draft.private.title"));
    List<String> privateLore = new ArrayList<>();
    privateLore.add(LanguageManager.langMessage("gui.draft.private.lore1"));
    privateLore.add(LanguageManager.langMessage("gui.draft.private.lore2"));
    privateMeta.setLore(privateLore);
    privateItem.setItemMeta(privateMeta);
    gui.setItem(PRIVATE_MATCHES_SLOT, privateItem);

    player.openInventory(gui);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) return;
    Player player = (Player) event.getWhoClicked();

    if (event.getView().getTitle().equals(LanguageManager.langMessage("gui.draft.title"))) {
      event.setCancelled(true);

      ItemStack clicked = event.getCurrentItem();
      if (clicked == null || clicked.getType() == Material.AIR) return;

      switch (event.getSlot()) {
        case BACK_SLOT: // Back button
          mainGUI.openMainMenu(player);
          break;
        case MIN_PLAYERS_SLOT: // Min players
          player.closeInventory();
          player.sendMessage(LanguageManager.langMessage("gui.draft.min_players.input"));
          waitingForInput.put(player.getUniqueId(), "min");
          break;
        case DRAFT_ORDER_SLOT: // Draft order
          player.closeInventory();
          player.sendMessage(LanguageManager.langMessage("gui.draft.order.input"));
          waitingForInput.put(player.getUniqueId(), "order");
          break;
        case PRIVATE_MATCHES_SLOT: // Private matches - disabled since it's map-specific
          player.sendMessage(LanguageManager.langMessage("gui.draft.private.disabled"));
          break;
        case SECOND_PICK_BALANCE_SLOT: // Second pick balance
          boolean currentSecondPick = ConfigManager.isSecondPickBalance();
          draftConfig.handleSecondGetsExtraPlayerCommand(player, !currentSecondPick);
          openDraftMenu(player);
          break;
        case SUGGESTIONS_SLOT: // Suggestions
          boolean currentSuggestions = ConfigManager.isDraftSuggestions();
          draftConfig.handleDraftSuggestionsCommand(player, !currentSuggestions);
          openDraftMenu(player);
          break;
        case TIMER_SLOT: // Timer
          boolean currentTimer = ConfigManager.isDraftTimer();
          draftConfig.handleDraftTimerCommand(player, !currentTimer);
          openDraftMenu(player);
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
      String message = event.getMessage();

      Bukkit.getScheduler().runTask(TowersForPGM.getInstance(), () -> {
        switch (inputType) {
          case "min":
            try {
              int minSize = Integer.parseInt(message);
              draftConfig.setMinDraftOrder(player, minSize);
            } catch (NumberFormatException e) {
              player.sendMessage(LanguageManager.langMessage("gui.error.invalid_number"));
            }
            break;
          case "order":
            draftConfig.setDraftOrder(player, message);
            break;
        }
        openDraftMenu(player);
      });
    }
  }
}
