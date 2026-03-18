package org.nicolie.towersforpgm.commands.towers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.commands.towers.commandUtils.RefillConfig;

import tc.oc.pgm.api.PGM;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.*;
import tc.oc.pgm.util.Audience;

public class RefillCommand {
  private final RefillConfig refillConfig = new RefillConfig();

  @Command("towers refill add")
  @CommandDescription("Add a refill chest at current location")
  public void refillAdd(Audience audience, Player sender) {
    Location loc = sender.getLocation();
    String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
    refillConfig.chest(audience, mapName, loc);
  }

  @Command("towers refill delete")
  @CommandDescription("Delete refill chest at current location")
  public void refillDelete(Audience audience, Player sender) {
    Location loc = sender.getLocation();
    String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
    refillConfig.remove(audience, mapName, loc);
  }

  @Command("towers refill reload")
  @CommandDescription("Reload refill chests for current map")
  public void refillReload(Audience audience, Player sender) {
    String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
    String worldName = sender.getWorld().getName();
    refillConfig.test(audience, mapName, worldName);
  }
}
