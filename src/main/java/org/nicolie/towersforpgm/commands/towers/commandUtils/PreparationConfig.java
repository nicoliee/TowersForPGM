package org.nicolie.towersforpgm.commands.towers.commandUtils;

import java.util.Map;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.preparationTime.Region;
import org.nicolie.towersforpgm.utils.MatchManager;

public class PreparationConfig {

  private final TowersForPGM plugin = TowersForPGM.getInstance();

  public void enabled(Audience audience, Boolean enabled) {
    boolean state =
        enabled != null ? enabled : plugin.config().preparationTime().isPreparationEnabled();

    if (enabled != null) {
      plugin.config().preparationTime().setPreparationEnabled(enabled);
    }

    String key = state ? "preparation.enabled" : "preparation.disabled";
    audience.sendMessage(Component.translatable(key));
  }

  public void add(Audience audience) {
    String mapName = MatchManager.getMatch().getMap().getName();

    if (plugin.config().preparationTime().hasRegion(mapName)) {
      audience.sendMessage(
          Component.translatable("region.mapAlreadyAdded", Component.text(mapName)));
      return;
    }

    Location p1 = new Location(Bukkit.getWorld(mapName), 0, 64, 0);
    Location p2 = new Location(Bukkit.getWorld(mapName), 100, 64, 100);

    Region region = new Region(p1, p2, 1, 1);

    plugin.config().preparationTime().addRegion(mapName, region);

    audience.sendMessage(Component.translatable("region.mapSuccess", Component.text(mapName)));
  }

  public void remove(Audience audience) {
    String mapName = MatchManager.getMatch().getMap().getName();

    if (!plugin.config().preparationTime().hasRegion(mapName)) {
      audience.sendMessage(Component.translatable("region.mapError", Component.text(mapName)));
      return;
    }

    plugin.config().preparationTime().removeRegion(mapName);

    audience.sendMessage(Component.translatable("region.mapDeleted", Component.text(mapName)));
  }

  public void min(Audience audience, String coordinates) {
    setLocation(audience, coordinates, false);
  }

  public void max(Audience audience, String coordinates) {
    setLocation(audience, coordinates, true);
  }

  private void setLocation(Audience audience, String coordinates, boolean max) {

    String mapName = MatchManager.getMatch().getMap().getName();
    World world = MatchManager.getMatch().getWorld();

    if (!plugin.config().preparationTime().hasRegion(mapName)) {
      audience.sendMessage(Component.translatable("preparation.noRegion", Component.text(mapName)));
      return;
    }

    Region region = plugin.config().preparationTime().getRegion(mapName);

    Location state = max ? region.getP2() : region.getP1();

    if (coordinates != null) {

      String[] split = coordinates.split(",");

      int x = Integer.parseInt(split[0]);
      int y = Integer.parseInt(split[1]);
      int z = Integer.parseInt(split[2]);

      Location newLocation = new Location(world, x, y, z);

      if (max) {
        plugin.config().preparationTime().updateRegionP2(mapName, newLocation);
      } else {
        plugin.config().preparationTime().updateRegionP1(mapName, newLocation);
      }

      state = newLocation;
    }

    String key = max ? "region.maxSet" : "region.minSet";

    audience.sendMessage(Component.translatable(
        key, Component.text(mapName), Component.text(formatLocation(state))));
  }

  public void timer(Audience audience, int timerValue) {

    String mapName = MatchManager.getMatch().getMap().getName();

    if (!plugin.config().preparationTime().hasRegion(mapName)) {
      audience.sendMessage(Component.translatable("region.mapError", Component.text(mapName)));
      return;
    }

    int state = plugin.config().preparationTime().getRegion(mapName).getTimer();

    if (timerValue != -1) {
      plugin.config().preparationTime().updateRegionTimer(mapName, timerValue);
      state = timerValue;
    }

    audience.sendMessage(
        Component.translatable("region.timerSet", Component.text(mapName), Component.text(state)));
  }

  public void haste(Audience audience, int hasteValue) {

    String mapName = MatchManager.getMatch().getMap().getName();

    if (!plugin.config().preparationTime().hasRegion(mapName)) {
      audience.sendMessage(Component.translatable("region.mapError", Component.text(mapName)));
      return;
    }

    int state = plugin.config().preparationTime().getRegion(mapName).getHaste();

    if (hasteValue != -1) {
      int haste = hasteValue;
      plugin.config().preparationTime().updateRegionHaste(mapName, haste);
      state = haste;
    }

    audience.sendMessage(
        Component.translatable("region.hasteSet", Component.text(mapName), Component.text(state)));
  }

  public void list(Audience audience) {

    Map<String, Region> regions = plugin.config().preparationTime().getRegions();

    if (regions.isEmpty()) {
      audience.sendMessage(Component.translatable("region.mapsNotFound"));
      return;
    }

    audience.sendMessage(Component.translatable("region.header"));

    for (Map.Entry<String, Region> entry : regions.entrySet()) {

      String mapName = entry.getKey();
      Region region = entry.getValue();

      audience.sendMessage(Component.text("§a" + mapName));
      audience.sendMessage(Component.text("§b  Min: §f" + formatLocation(region.getP1())));
      audience.sendMessage(Component.text("§b  Max: §f" + formatLocation(region.getP2())));
      audience.sendMessage(Component.text("§b  Timer: §f" + region.getTimer()));
      audience.sendMessage(Component.text("§b  Haste: §f" + region.getHaste()));
    }
  }

  private String formatLocation(Location location) {
    if (location == null) return "Not set";
    return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
  }
}
