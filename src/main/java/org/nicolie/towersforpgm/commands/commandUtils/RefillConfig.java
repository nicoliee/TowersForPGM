package org.nicolie.towersforpgm.commands.commandUtils;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

public class RefillConfig {
  private final TowersForPGM plugin = TowersForPGM.getInstance();

  public void addRefillLocation(CommandSender sender, String mapName, Location loc) {
    FileConfiguration refillConfig = plugin.config().refill().config();

    // Verificar si las coordenadas corresponden a un cofre
    Block block = loc.getBlock();
    String coords = loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    if (!(block.getState() instanceof Chest)) {
      SendMessage.sendToPlayer(
          sender, LanguageManager.message("refill.chestNotFound").replace("{coords}", coords));
      return;
    }

    // Verificar si el cofre ya está en la configuración
    for (int i = 1; refillConfig.contains("refill." + mapName + ".c" + i); i++) {
      String storedCoords = refillConfig.getString("refill." + mapName + ".c" + i);
      if (storedCoords.equals(coords)) {
        SendMessage.sendToPlayer(
            sender,
            LanguageManager.message("refill.chestAlreadyExists").replace("{coords}", coords));
        return;
      }
    }

    // Si el mapa no tiene una sección, crearla
    if (!refillConfig.contains("refill." + mapName)) {
      refillConfig.createSection("refill." + mapName);
    }

    // Contar cuántas coordenadas hay para este mapa (c1, c2, ...)
    int i = 1;
    while (refillConfig.contains("refill." + mapName + ".c" + i)) {
      i++;
    }

    // Agregar la nueva coordenada
    refillConfig.set("refill." + mapName + ".c" + i, coords);
    SendMessage.sendToPlayer(
        sender, LanguageManager.message("refill.chestSet").replace("{coords}", coords));
    plugin.config().refill().save();
    plugin.config().refill().reload();
  }

  public void removeRefillLocation(CommandSender sender, String mapName, Location loc) {
    FileConfiguration refillConfig = plugin.config().refill().config();
    String coords = loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    boolean found = false;
    for (int i = 1; refillConfig.contains("refill." + mapName + ".c" + i); i++) {
      String storedCoords = refillConfig.getString("refill." + mapName + ".c" + i);
      if (storedCoords.equals(coords)) {
        refillConfig.set("refill." + mapName + ".c" + i, null);
        plugin.config().refill().save();
        plugin.config().refill().reload();
        SendMessage.sendToPlayer(
            sender, LanguageManager.message("refill.chestDeleted").replace("{coords}", coords));
        found = true;
        break;
      }
    }

    if (!found) {
      SendMessage.sendToPlayer(
          sender, LanguageManager.message("refill.chestNotFound").replace("{coords}", coords));
    }
  }

  public void testRefill(CommandSender sender, String mapName, String world) {
    RefillManager refillManager = TowersForPGM.getInstance().getRefillManager();
    refillManager.loadChests(mapName, world);
  }
}
