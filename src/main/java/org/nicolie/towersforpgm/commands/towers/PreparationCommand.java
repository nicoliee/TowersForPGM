package org.nicolie.towersforpgm.commands.towers;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.commands.towers.commandUtils.PreparationConfig;
import org.nicolie.towersforpgm.utils.Permissions;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.*;
import tc.oc.pgm.util.Audience;

public class PreparationCommand {
  private final PreparationConfig preparationConfig = new PreparationConfig();

  @Command("towers preparation add")
  // @CommandDescription("Add a preparation zone")
  @Permission(Permissions.ADMIN)
  public void preparationAdd(Audience audience, Player sender) {
    preparationConfig.add(audience);
  }

  @Command("towers preparation haste [level]")
  // @CommandDescription("Set preparation haste level")
  @Permission(Permissions.ADMIN)
  public void preparationHaste(Audience audience, Player sender, @Argument("level") Integer level) {
    preparationConfig.haste(audience, level == null ? -1 : level);
  }

  @Command("towers preparation list")
  // @CommandDescription("List preparation zones")
  @Permission(Permissions.ADMIN)
  public void preparationList(Audience audience, CommandSender sender) {
    preparationConfig.list(audience);
  }

  @Command("towers preparation max <x> <y> <z>")
  // @CommandDescription("Set preparation max corner")
  @Permission(Permissions.ADMIN)
  public void preparationMax(
      Audience audience,
      Player sender,
      @Argument("x") int x,
      @Argument("y") int y,
      @Argument("z") int z) {
    preparationConfig.max(audience, x + "," + y + "," + z);
  }

  @Command("towers preparation min <x> <y> <z>")
  // @CommandDescription("Set preparation min corner")
  @Permission(Permissions.ADMIN)
  public void preparationMin(
      Audience audience,
      Player sender,
      @Argument("x") int x,
      @Argument("y") int y,
      @Argument("z") int z) {
    preparationConfig.min(audience, x + "," + y + "," + z);
  }

  @Command("towers preparation remove")
  // @CommandDescription("Remove a preparation zone")
  @Permission(Permissions.ADMIN)
  public void preparationRemove(Audience audience, Player sender) {
    preparationConfig.remove(audience);
  }

  @Command("towers preparation timer [mins]")
  // @CommandDescription("Set preparation timer")
  @Permission(Permissions.ADMIN)
  public void preparationTimer(
      Audience audience, CommandSender sender, @Argument("mins") Integer mins) {
    preparationConfig.timer(audience, mins == null ? -1 : mins);
  }

  @Command("towers preparation toggle [value]")
  // @CommandDescription("Toggle preparation phase")
  @Permission(Permissions.ADMIN)
  public void preparationToggle(
      Audience audience, CommandSender sender, @Argument("value") Boolean value) {
    preparationConfig.enabled(audience, value);
  }
}
