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

// Aunque PGM no soporta por el momento varios mapas a la vez, se implementa la funcionalidad
// para cargar cofres de recarga en varios mapas, por si en el futuro se implementa esta
// funcionalidad
// Se implementa la funcionalidad de recarga de cofres en un intervalo de tiempo de 60 segundos

public class RefillManager {
  private final LanguageManager languageManager;
  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final Map<String, Map<Location, ItemStack[]>> chestContents = new HashMap<>();
  private final Map<String, BukkitRunnable> refillTasks = new HashMap<>();

  // Variable para almacenar el último tiempo de recarga

  public RefillManager(LanguageManager languageManager) {
    this.languageManager = languageManager;
  }

  public void loadChests(String mapName, String worldName) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      FileConfiguration refillConfig = plugin.getRefillConfig();
      ConfigurationSection refillSection =
          refillConfig.getConfigurationSection("refill." + mapName);
      if (refillSection == null) {
        return; // No se encontró sección de recarga para este mapa, no hacer nada
      } else {
        // Enviar mensaje desde el hilo principal
        Bukkit.getScheduler().runTask(plugin, () -> {
          SendMessage.sendToAdmins(
              languageManager.getPluginMessage("refill.mapFound").replace("{map}", mapName));
        });
      }

      World world = Bukkit.getWorld(worldName);
      if (world == null) {
        return; // No se encontró el mundo, no hacer nada
      }

      // Recopilar todas las ubicaciones en el hilo asíncrono
      List<Location> chestLocations = new ArrayList<>();

      for (String key : refillSection.getKeys(false)) {
        String coordsString = refillSection.getString(key);
        if (coordsString == null || coordsString.isEmpty()) {
          continue; // Coordenadas vacías, saltar este cofre
        }

        // Dividir la cadena en partes y convertirlas a enteros
        String[] coordsArray = coordsString.split(", ");
        if (coordsArray.length != 3) {
          continue; // Coordenadas inválidas, saltar este cofre
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

      // Procesar todas las ubicaciones en el hilo principal
      if (!chestLocations.isEmpty()) {
        Bukkit.getScheduler().runTask(plugin, () -> {
          Map<Location, ItemStack[]> chests = new HashMap<>();

          for (Location loc : chestLocations) {
            // Depuración: verificar el tipo de bloque
            Material blockType = loc.getBlock().getType();
            // Verificar si el bloque en la ubicación es un cofre
            if (blockType == Material.CHEST || blockType == Material.TRAPPED_CHEST) {
              Inventory chestInv =
                  ((org.bukkit.block.Chest) loc.getBlock().getState()).getBlockInventory();
              chests.put(loc, chestInv.getContents());
            } else {
              // Enviar mensaje a los administradores
              String message = "&c¡Advertencia! No hay cofre en la ubicación: "
                  + "x=" + loc.getBlockX()
                  + ", y=" + loc.getBlockY()
                  + ", z=" + loc.getBlockZ()
                  + " bloque: " + blockType;
              SendMessage.sendToAdmins(message);
            }
          }

          // Guardar los cofres después de procesarlos todos
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
    // Verificar si existen cofres cargados para el mundo
    if (!chestContents.containsKey(worldName)) {
      return; // No hay cofres cargados, no hacer nada
    }

    // Si ya hay una tarea corriendo para este mundo, la detenemos primero
    if (refillTasks.containsKey(worldName)) {
      refillTasks.get(worldName).cancel();
    }

    BukkitRunnable task = new BukkitRunnable() {
      @Override
      public void run() {
        refill(worldName);
      }
    };

    // Guardamos la tarea en el mapa y la iniciamos
    refillTasks.put(worldName, task);
    task.runTaskTimer(plugin, 0L, 60 * 20);
  }

  public void refill(String worldName) {
    // Verificamos si hay cofres cargados para el mundo antes de intentar acceder a ellos
    Map<Location, ItemStack[]> chests = chestContents.get(worldName);
    if (chests == null) {
      return; // Si no hay cofres para este mundo, salir
    }

    // Si existen cofres, procedemos con el relleno
    for (Map.Entry<Location, ItemStack[]> entry : chests.entrySet()) {
      Location loc = entry.getKey();
      if (loc.getBlock().getType() == Material.CHEST) {
        Inventory chestInv =
            ((org.bukkit.block.Chest) loc.getBlock().getState()).getBlockInventory();

        // Validar y ajustar cada ItemStack antes de ponerlo en el cofre
        ItemStack[] items = entry.getValue();
        for (int i = 0; i < items.length; i++) {
          ItemStack item = items[i];
          if (item != null && item.getAmount() == 0) {
            items[i] = null; // Eliminar los ítems con cantidad 0
          }
        }

        chestInv.setContents(items);
      }
    }
  }

  // Método para detener la tarea de recarga de cofres
  public void stopRefillTask(String worldName) {
    if (refillTasks.containsKey(worldName)) {
      refillTasks.get(worldName).cancel();
      refillTasks.remove(worldName);
    }
  }
}
