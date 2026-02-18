package org.nicolie.towersforpgm.draft.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.models.Stats;
import org.nicolie.towersforpgm.draft.core.AvailablePlayers;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Draft;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.rankeds.Rank;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class PicksGUI implements Listener {

  private final TowersForPGM plugin;
  private final Draft draft;
  private final Captains captains;
  private final AvailablePlayers availablePlayers;
  private final Teams teams;

  // Para saber qué inventario pertenece a qué capitán
  private final Map<UUID, Inventory> openInventories = new HashMap<>();

  public PicksGUI(
      TowersForPGM plugin,
      Draft draft,
      Captains captains,
      AvailablePlayers availablePlayers,
      Teams teams) {
    this.plugin = plugin;
    this.draft = draft;
    this.captains = captains;
    this.availablePlayers = availablePlayers;
    this.teams = teams;
  }

  public void openInventory(Player player) {
    // Combinar y ordenar jugadores (online y offline) alfabéticamente
    Set<String> nameSet = new HashSet<>();
    for (MatchPlayer p : availablePlayers.getAvailablePlayers()) {
      nameSet.add(p.getNameLegacy());
    }
    nameSet.addAll(availablePlayers.getAvailableOfflinePlayers());

    List<String> allPlayerNames = new ArrayList<>(nameSet);
    allPlayerNames.sort(String::compareToIgnoreCase);

    int totalPlayers = allPlayerNames.size();

    Inventory inv;
    int columnsPerRow;
    int currentIndex = 0;
    int captainNumber = captains.getCaptainTeam(player.getUniqueId());

    // === CASO 1: <= 28 jugadores ===
    if (totalPlayers <= 28) {
      int inventorySize = getInventorySizeWithBorder(totalPlayers);
      inv = Bukkit.createInventory(
          null,
          inventorySize,
          LanguageManager.message("draft.config.inventoryName")
              .replace("{size}", String.valueOf(totalPlayers)));

      // Cambiar el color e los cristales según las condiciones
      for (int i = 0; i < inventorySize; i++) {
        if (i < 9 || i >= inventorySize - 9 || i % 9 == 0 || i % 9 == 8) {
          ItemStack glassPane =
              new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 15); // Vidrio negro por defecto

          if (captainNumber == 1) {
            if (captains.isCaptain1Turn()) {
              glassPane =
                  new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 5); // Verde si es su turno
            } else {
              glassPane = new ItemStack(
                  Material.STAINED_GLASS_PANE, 1, (byte) 14); // Rojo si no es su turno
            }
          } else if (captainNumber == 2) {
            if (captains.isCaptain1Turn()) {
              glassPane = new ItemStack(
                  Material.STAINED_GLASS_PANE, 1, (byte) 11); // Azul si no es su turno
            } else {
              glassPane =
                  new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 5); // Verde si es su turno
            }
          }

          ItemMeta glassMeta = glassPane.getItemMeta();
          glassMeta.setDisplayName(" ");
          glassPane.setItemMeta(glassMeta);
          inv.setItem(i, glassPane);
        }
      }

      columnsPerRow = 7; // quitamos 2 slots por los bordes
      int rowOffset = 9;

      for (String name : allPlayerNames) {
        boolean isOnline = Bukkit.getPlayerExact(name) != null;

        ItemStack skull = createPlayerSkull(name, isOnline);

        if (plugin.getIsDatabaseActivated()) {
          Stats stats = availablePlayers.getStatsForPlayer(name);
          addSkullLore(skull, stats);
        }
        int row = currentIndex / columnsPerRow;
        int col = currentIndex % columnsPerRow;
        int slot = rowOffset + row * 9 + (col + 1); // +1 para saltar el primer borde

        int finalSlot = slot;
        Bukkit.getScheduler().runTask(plugin, () -> {
          inv.setItem(finalSlot, skull);
        });

        currentIndex++;
      }

    } else {
      // === CASO 2: >= 29 jugadores ===
      int inventorySize = getInventorySizeWithoutBorders(totalPlayers);
      inv = Bukkit.createInventory(
          null,
          inventorySize,
          LanguageManager.message("draft.config.inventoryName")
              .replace("{size}", String.valueOf(totalPlayers)));
      columnsPerRow = 9;

      for (String name : allPlayerNames) {
        boolean isOnline = Bukkit.getPlayerExact(name) != null;

        ItemStack skull = createPlayerSkull(name, isOnline);

        if (plugin.getIsDatabaseActivated()) {
          Stats stats = availablePlayers.getStatsForPlayer(name);
          addSkullLore(skull, stats);
        }

        int slot = currentIndex;
        Bukkit.getScheduler().runTask(plugin, () -> {
          inv.setItem(slot, skull);
        });

        currentIndex++;
      }
    }

    player.openInventory(inv);
    openInventories.put(player.getUniqueId(), inv);
  }

  // Calcula tamaño de inventario con bordes decorativos
  private int getInventorySizeWithBorder(int itemCount) {
    int columns = 7;
    int rows = (int) Math.ceil(itemCount / (double) columns);
    rows = Math.max(1, Math.min(rows, 4));
    return (rows + 2) * 9; // +2 para bordes
  }

  // Calcula tamaño de inventario SIN bordes
  private int getInventorySizeWithoutBorders(int itemCount) {
    int columns = 9;
    int rows = (int) Math.ceil(itemCount / (double) columns);
    rows = Math.max(1, Math.min(rows, 6)); // máximo 6 filas (54 slots)
    return rows * 9;
  }

  // Crea la cabeza del jugador
  private ItemStack createPlayerSkull(String name, boolean isOnline) {
    ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
    SkullMeta meta = (SkullMeta) skull.getItemMeta();
    meta.setOwner(name);
    meta.setDisplayName((isOnline ? "§6" : "§7") + name);
    skull.setItemMeta(meta);
    return skull;
  }

  // Añade lore con estadísticas
  private void addSkullLore(ItemStack skull, Stats stats) {
    SkullMeta meta = (SkullMeta) skull.getItemMeta();
    List<String> lore = new ArrayList<>();
    int elo = stats.getElo();
    if (elo != -9999) {
      Rank rank = Rank.getRankByElo(elo);
      lore.add(LanguageManager.message("stats.elo") + ": " + rank.getPrefixedRank(true) + " "
          + rank.getColor() + elo);
      lore.add(" ");
    }
    double kd =
        stats.getDeaths() > 0 ? (double) stats.getKills() / stats.getDeaths() : stats.getKills();
    lore.add("§7" + LanguageManager.message("stats.kills") + ": §a" + stats.getKills() + " §7"
        + LanguageManager.message("stats.deaths") + ": §c" + stats.getDeaths() + " §7KD: §a"
        + String.format("%.2f", kd));

    lore.add("§7" + LanguageManager.message("stats.games") + ": §a" + stats.getGames() + " §7"
        + LanguageManager.message("stats.wins") + ": §a" + stats.getWins() + " §7"
        + LanguageManager.message("stats.winstreak") + ": §a" + stats.getWinstreak());

    double avgDamageDone = stats.getGames() > 0 ? stats.getDamageDone() / stats.getGames() : 0.0;
    double avgDamageTaken = stats.getGames() > 0 ? stats.getDamageTaken() / stats.getGames() : 0.0;
    lore.add("§7" + LanguageManager.message("stats.damageDone") + ": §a"
        + String.format("%.1f", avgDamageDone) + " ❤ §7"
        + LanguageManager.message("stats.damageTaken") + ": §c"
        + String.format("%.1f", avgDamageTaken) + " ❤");
    lore.add("§7" + LanguageManager.message("stats.points") + ": §6" + stats.getPoints());
    lore.add(" ");
    lore.add(LanguageManager.message("draft.config.clickToPick"));
    meta.setLore(lore);
    skull.setItemMeta(meta);
  }

  private String validatePlayerToPick(String inputName, UUID clickerId) {
    MatchPlayer pickedPlayer = availablePlayers.getAvailablePlayers().stream()
        .filter(p -> p.getNameLegacy().equalsIgnoreCase(inputName))
        .findFirst()
        .orElse(null);

    String pickedPlayerString = null;
    if (pickedPlayer != null) {
      pickedPlayerString = pickedPlayer.getNameLegacy();
    } else {
      pickedPlayerString = availablePlayers.getAvailableOfflinePlayers().stream()
          .filter(name -> name.equalsIgnoreCase(inputName))
          .findFirst()
          .orElse(null);
    }

    if (pickedPlayerString == null) {
      return LanguageManager.message("draft.picks.notInList").replace("{player}", inputName);
    }

    if (teams.isPlayerInAnyTeam(pickedPlayerString)) {
      return LanguageManager.message("draft.picks.alreadyPicked")
          .replace("{player}", pickedPlayerString);
    }

    return null;
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    Player clicker = (Player) event.getWhoClicked();
    UUID clickerId = clicker.getUniqueId();

    if (!openInventories.containsKey(clickerId)) return;

    Inventory inv = openInventories.get(clickerId);
    if (!event.getInventory().equals(inv)) return;

    event.setCancelled(true);

    ItemStack clicked = event.getCurrentItem();
    if (clicked == null || clicked.getType() != Material.SKULL_ITEM) return;

    SkullMeta meta = (SkullMeta) clicked.getItemMeta();
    if (meta == null || meta.getOwner() == null) return;
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(clicker);

    // Prevenir picks durante la fase de reroll o captains
    DraftPhase currentPhase = Draft.getPhase();
    if (currentPhase == DraftPhase.CAPTAINS || currentPhase == DraftPhase.REROLL) {
      openInventories.remove(clickerId);
      clicker.closeInventory();
      matchPlayer.sendWarning(Component.text(LanguageManager.message("draft.picks.notTurn")));
      return;
    }

    int captainNumber = captains.getCaptainTeam(clickerId);
    if (captainNumber == -1) {
      openInventories.remove(clickerId);
      clicker.closeInventory();
      matchPlayer.sendWarning(Component.text(LanguageManager.message("draft.picks.notCaptain")));
      return;
    }

    if ((captains.isCaptain1Turn() && captainNumber == 2)
        || (!captains.isCaptain1Turn() && captainNumber == 1)) {
      openInventories.remove(clickerId);
      clicker.closeInventory();
      matchPlayer.sendWarning(Component.text(LanguageManager.message("draft.picks.notTurn")));
      return;
    }

    String inputName = meta.getOwner();
    String validationError = validatePlayerToPick(inputName, clickerId);
    if (validationError != null) {
      openInventories.remove(clickerId);
      clicker.closeInventory();
      matchPlayer.sendWarning(Component.text(validationError));
      return;
    }

    // Ejecutar pick
    draft.pickPlayer(inputName);
    clicker.closeInventory();
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    Player player = (Player) event.getPlayer();
    UUID playerId = player.getUniqueId();

    if (openInventories.containsKey(playerId)) {
      openInventories.remove(playerId);
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    openInventories.remove(event.getPlayer().getUniqueId());
  }

  public void updateInventory(Player player) {
    Inventory inv = openInventories.get(player.getUniqueId());
    if (inv != null) {
      inv.clear();
      openInventory(player);
    }
  }

  public void updateAllInventories() {
    for (UUID playerId : new HashSet<>(openInventories.keySet())) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null && player.isOnline()) {
        updateInventory(player);
      }
    }
  }

  public void giveItemToPlayers(World world) {
    for (Player player : world.getPlayers()) {
      giveItemToPlayer(player);
    }
  }

  public void giveItemToMatch(Match match) {
    for (MatchPlayer matchPlayer : match.getPlayers()) {
      Player player = matchPlayer.getBukkit();
      if (player != null && player.isOnline()) {
        giveItemToPlayer(player);
      }
    }
  }

  public void giveItemToPlayer(Player player) {
    ItemStack specialItem = new ItemStack(Material.NETHER_STAR);
    ItemMeta meta = specialItem.getItemMeta();
    meta.setDisplayName("§6Draft Menu");
    meta.setLore(Collections.singletonList(LanguageManager.message("draft.picks.itemLore")));
    specialItem.setItemMeta(meta);

    player.getInventory().setItem(2, null);
    player.getInventory().setItem(2, specialItem);
  }

  public void removeItemToPlayers(World world) {
    for (Player player : world.getPlayers()) {
      removeItemToPlayer(player);
    }
  }

  public void removeItemToPlayer(Player player) {
    for (ItemStack item : player.getInventory().getContents()) {
      if (item != null && item.getType() == Material.NETHER_STAR && item.hasItemMeta()) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && "§6Draft Menu".equals(meta.getDisplayName())) {
          player.getInventory().remove(item);
        }
      }
    }
  }

  @SuppressWarnings("deprecation")
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();

    if (event.getAction() == Action.RIGHT_CLICK_AIR
        || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
      ItemStack item;
      try {
        item = player.getInventory().getItemInMainHand();
      } catch (NoSuchMethodError e) {
        item = player.getItemInHand();
      }

      if (item != null && item.hasItemMeta()) {
        ItemMeta meta = item.getItemMeta();

        if (item.getType() == Material.NETHER_STAR
            && meta != null
            && "§6Draft Menu".equals(meta.getDisplayName())) {
          if (Draft.getPhase() == DraftPhase.IDLE) {
            SendMessage.sendToPlayer(player, LanguageManager.message("draft.picks.noDraft"));
            player.getInventory().remove(Material.NETHER_STAR);
            return;
          }
          openInventory(player);
          event.setCancelled(true);
        }
      }
    }
  }
}
