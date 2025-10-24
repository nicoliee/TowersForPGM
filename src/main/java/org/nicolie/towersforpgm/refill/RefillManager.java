package org.nicolie.towersforpgm.refill;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

public class RefillManager {
  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final Map<String, Map<Location, ItemStack[]>> chestContents = new HashMap<>();
  private final Map<String, BukkitRunnable> refillTasks = new HashMap<>();

  public void loadChests(String mapName, String worldName) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      FileConfiguration refillConfig = plugin.getRefillConfig();
      ConfigurationSection refillSection =
          refillConfig.getConfigurationSection("refill." + mapName);
      if (refillSection == null) {
        return;
      } else {
        Bukkit.getScheduler().runTask(plugin, () -> {
          SendMessage.sendToAdmins(
              LanguageManager.langMessage("refill.mapFound").replace("{map}", mapName));
        });
      }

      World world = Bukkit.getWorld(worldName);
      if (world == null) {
        return;
      }

      List<Location> chestLocations = new ArrayList<>();

      for (String key : refillSection.getKeys(false)) {
        String coordsString = refillSection.getString(key);
        if (coordsString == null || coordsString.isEmpty()) {
          continue;
        }

        String[] coordsArray = coordsString.split(", ");
        if (coordsArray.length != 3) {
          continue;
        }

        try {
          int x = Integer.parseInt(coordsArray[0].trim());
          int y = Integer.parseInt(coordsArray[1].trim());
          int z = Integer.parseInt(coordsArray[2].trim());
          Location loc = new Location(world, x, y, z);
          chestLocations.add(loc);
        } catch (NumberFormatException e) {
          continue;
        }
      }

      if (!chestLocations.isEmpty()) {
        Bukkit.getScheduler().runTask(plugin, () -> {
          Map<Location, ItemStack[]> chests = new HashMap<>();

          for (Location loc : chestLocations) {
            Material blockType = loc.getBlock().getType();
            if (blockType == Material.CHEST || blockType == Material.TRAPPED_CHEST) {
              Inventory chestInv =
                  ((org.bukkit.block.Chest) loc.getBlock().getState()).getBlockInventory();
              chests.put(loc, chestInv.getContents());
            } else {
              String message = "&c¡Advertencia! No hay cofre en la ubicación: "
                  + "x=" + loc.getBlockX()
                  + ", y=" + loc.getBlockY()
                  + ", z=" + loc.getBlockZ()
                  + " bloque: " + blockType;
              SendMessage.sendToAdmins(message);
            }
          }

          if (!chests.isEmpty()) {
            chestContents.put(worldName, chests);
          }
        });
      }
    });
  }

  public void clearWorldData(String worldName) {
    chestContents.remove(worldName);
    stopRefillTask(worldName);
  }

  public void startRefillTask(String worldName) {
    if (!chestContents.containsKey(worldName)) {
      return;
    }

    if (refillTasks.containsKey(worldName)) {
      refillTasks.get(worldName).cancel();
    }

    BukkitRunnable task = new BukkitRunnable() {
      @Override
      public void run() {
        refill(worldName);
      }
    };

    refillTasks.put(worldName, task);
    task.runTaskTimer(plugin, 0L, 60 * 20);
  }

  public void refill(String worldName) {
    Map<Location, ItemStack[]> chests = chestContents.get(worldName);
    if (chests == null) {
      return;
    }

    for (Map.Entry<Location, ItemStack[]> entry : chests.entrySet()) {
      Location loc = entry.getKey();
      if (loc.getBlock().getType() == Material.CHEST) {
        Inventory chestInv =
            ((org.bukkit.block.Chest) loc.getBlock().getState()).getBlockInventory();

        ItemStack[] items = entry.getValue();
        for (int i = 0; i < items.length; i++) {
          ItemStack item = items[i];
          if (item != null && item.getAmount() == 0) {
            items[i] = null;
          }
        }

        chestInv.setContents(items);
      }
    }
  }

  public void stopRefillTask(String worldName) {
    if (refillTasks.containsKey(worldName)) {
      refillTasks.get(worldName).cancel();
      refillTasks.remove(worldName);
    }
  }
}
