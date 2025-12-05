package org.nicolie.towersforpgm.commands.commandUtils;

import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.preparationTime.Region;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;

public class PreparationConfig {
  private final TowersForPGM plugin = TowersForPGM.getInstance();

  public void togglePreparation(CommandSender sender) {
    boolean newState = !plugin.config().preparationTime().isPreparationEnabled();
    plugin.config().preparationTime().setPreparationEnabled(newState);
    String messageKey = newState ? "preparation.enabled" : "preparation.disabled";
    SendMessage.sendToPlayer(sender, LanguageManager.message(messageKey));
  }

  public void handleAddCommand(CommandSender sender) {
    String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();

    // Verificar si ya existe la región
    if (plugin.config().preparationTime().hasRegion(mapName)) {
      SendMessage.sendToPlayer(
          sender, LanguageManager.message("region.mapAlreadyAdded").replace("{map}", mapName));
      return;
    }

    // Asignar valores por defecto a la región
    Location p1 = new Location(Bukkit.getWorld(mapName), 0, 64, 0); // Coordenadas P1 por defecto
    Location p2 =
        new Location(Bukkit.getWorld(mapName), 100, 64, 100); // Coordenadas P2 por defecto
    int timer = 1; // Temporizador por defecto (1 minuto)
    int haste = 1; // Haste por defecto (1 minuto)

    // Crear la región
    Region region = new Region(p1, p2, timer, haste);

    // Agregar la región (actualiza tanto config.yml como memoria)
    plugin.config().preparationTime().addRegion(mapName, region);

    SendMessage.sendToPlayer(
        sender, LanguageManager.message("region.mapSuccess").replace("{map}", mapName));
  }

  public void handleRemoveCommand(CommandSender sender) {
    String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();

    // Verificar si existe la región
    if (!plugin.config().preparationTime().hasRegion(mapName)) {
      SendMessage.sendToPlayer(
          sender, LanguageManager.message("region.mapError").replace("{map}", mapName));
      return;
    }

    // Eliminar la región (actualiza tanto config.yml como memoria)
    plugin.config().preparationTime().removeRegion(mapName);

    SendMessage.sendToPlayer(
        sender, LanguageManager.message("region.mapDeleted").replace("{map}", mapName));
  }

  public void handleCoordinates(CommandSender sender, boolean isMax, int x, int y, int z) {
    String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
    World world = PGM.get().getMatchManager().getMatch(sender).getWorld();

    // Verificar si existe la región
    if (!plugin.config().preparationTime().hasRegion(mapName)) {
      SendMessage.sendToPlayer(
          sender, LanguageManager.message("preparation.noRegion").replace("{map}", mapName));
      return;
    }

    Location newLocation = new Location(world, x, y, z);

    // Actualizar las coordenadas usando los métodos que sincronizan config.yml y memoria
    if (isMax) {
      plugin.config().preparationTime().updateRegionP2(mapName, newLocation);
    } else {
      plugin.config().preparationTime().updateRegionP1(mapName, newLocation);
    }

    String message = isMax ? "region.maxSet" : "region.minSet";
    SendMessage.sendToPlayer(
        sender,
        LanguageManager.message(message)
            .replace("{map}", mapName)
            .replace("{x}", String.valueOf(x))
            .replace("{y}", String.valueOf(y))
            .replace("{z}", String.valueOf(z)));
  }

  public void handleTimerCommand(CommandSender sender, int timer) {
    String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();

    // Verificar si existe la región
    if (!plugin.config().preparationTime().hasRegion(mapName)) {
      SendMessage.sendToPlayer(
          sender, LanguageManager.message("region.mapError").replace("{map}", mapName));
      return;
    }
    if (timer <= 0) {
      SendMessage.sendToPlayer(sender, LanguageManager.message("region.usage"));
      return;
    }

    // Actualizar el timer (sincroniza config.yml y memoria)
    plugin.config().preparationTime().updateRegionTimer(mapName, timer);

    SendMessage.sendToPlayer(
        sender,
        LanguageManager.message("region.timerSet")
            .replace("{map}", mapName)
            .replace("{timer}", SendMessage.formatTime(timer * 60)));
  }

  public void handleHasteCommand(CommandSender sender, int haste) {
    String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();

    // Verificar si existe la región
    if (!plugin.config().preparationTime().hasRegion(mapName)) {
      SendMessage.sendToPlayer(sender, LanguageManager.message("region.mapError"));
      return;
    }
    if (haste <= 0) {
      SendMessage.sendToPlayer(sender, LanguageManager.message("region.usage"));
      return;
    }

    // Actualizar el haste (sincroniza config.yml y memoria)
    plugin.config().preparationTime().updateRegionHaste(mapName, haste);

    SendMessage.sendToPlayer(
        sender,
        LanguageManager.message("region.hasteSet")
            .replace("{map}", mapName)
            .replace("{timer}", SendMessage.formatTime(haste * 60)));
  }

  public void handleListCommand(CommandSender sender) {
    Map<String, Region> regions = plugin.config().preparationTime().getRegions();

    if (regions.isEmpty()) {
      SendMessage.sendToPlayer(sender, LanguageManager.message("region.mapsNotFound"));
      return;
    }

    SendMessage.sendToPlayer(sender, LanguageManager.message("region.header"));
    for (Map.Entry<String, Region> entry : regions.entrySet()) {
      String mapName = entry.getKey();
      Region region = entry.getValue();
      String database = plugin.config().databaseTables().getTable(mapName);

      // Formatear coordenadas de las ubicaciones
      String p1 = formatLocation(region.getP1());
      String p2 = formatLocation(region.getP2());

      sender.sendMessage(ChatColor.GREEN + mapName);
      sender.sendMessage(ChatColor.AQUA + "  Base de datos: " + ChatColor.WHITE + database);
      sender.sendMessage(ChatColor.AQUA + "  Coordenadas mínimas: " + ChatColor.WHITE + p1);
      sender.sendMessage(ChatColor.AQUA + "  Coordenadas máximas: " + ChatColor.WHITE + p2);
      sender.sendMessage(ChatColor.AQUA + "  Temporizador: " + ChatColor.WHITE
          + (region.getTimer() > 0 ? region.getTimer() + " minutos" : "No definido"));
      sender.sendMessage(ChatColor.AQUA + "  Temporizador de haste: " + ChatColor.WHITE
          + (region.getHaste() > 0 ? region.getHaste() + " minutos" : "No definido"));
    }
  }

  private String formatLocation(Location location) {
    if (location == null) {
      return "No definido";
    }
    return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
  }
}
