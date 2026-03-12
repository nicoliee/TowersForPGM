package org.nicolie.towersforpgm.commands.commandUtils;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.refill.RefillManager;

public class RefillConfig {

  private final TowersForPGM plugin = TowersForPGM.getInstance();

  public void chest(Audience audience, String mapName, Location location) {

    FileConfiguration config = plugin.config().refill().config();

    if (location == null) {
      audience.sendMessage(Component.translatable("refill.usage"));
      return;
    }

    Block block = location.getBlock();

    String coords =
        location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();

    if (!(block.getState() instanceof Chest)) {
      audience.sendMessage(Component.translatable("refill.chestNotFound", Component.text(coords)));
      return;
    }

    for (int i = 1; config.contains("refill." + mapName + ".c" + i); i++) {

      String stored = config.getString("refill." + mapName + ".c" + i);

      if (stored.equals(coords)) {

        audience.sendMessage(
            Component.translatable("refill.chestAlreadyExists", Component.text(coords)));
        return;
      }
    }

    if (!config.contains("refill." + mapName)) {
      config.createSection("refill." + mapName);
    }

    int i = 1;

    while (config.contains("refill." + mapName + ".c" + i)) {
      i++;
    }

    config.set("refill." + mapName + ".c" + i, coords);

    plugin.config().refill().save();
    plugin.config().refill().reload();

    audience.sendMessage(Component.translatable("refill.chestSet", Component.text(coords)));
  }

  public void remove(Audience audience, String mapName, Location location) {

    FileConfiguration config = plugin.config().refill().config();

    if (location == null) {
      audience.sendMessage(Component.translatable("refill.usage"));
      return;
    }

    String coords =
        location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();

    boolean found = false;

    for (int i = 1; config.contains("refill." + mapName + ".c" + i); i++) {

      String stored = config.getString("refill." + mapName + ".c" + i);

      if (stored.equals(coords)) {

        config.set("refill." + mapName + ".c" + i, null);

        plugin.config().refill().save();
        plugin.config().refill().reload();

        audience.sendMessage(Component.translatable("refill.chestDeleted", Component.text(coords)));

        found = true;
        break;
      }
    }

    if (!found) {
      audience.sendMessage(Component.translatable("refill.chestNotFound", Component.text(coords)));
    }
  }

  public void test(Audience audience, String mapName, String world) {

    RefillManager refillManager = plugin.getRefillManager();

    refillManager.loadChests(mapName, world);

    audience.sendMessage(Component.translatable("refill.test"));
  }
}
