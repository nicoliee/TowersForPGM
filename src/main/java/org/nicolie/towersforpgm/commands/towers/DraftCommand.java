package org.nicolie.towersforpgm.commands.towers;

import org.bukkit.command.CommandSender;
import org.nicolie.towersforpgm.commands.towers.commandUtils.DraftConfig;

import tc.oc.pgm.lib.org.incendo.cloud.annotations.*;
import tc.oc.pgm.util.Audience;

public class DraftCommand {
  private final DraftConfig draftConfig = new DraftConfig();

  @Command("towers draft min [value]")
  @CommandDescription("Set draft min order")
  public void draftMin(Audience audience, CommandSender sender, @Argument("value") Integer value) {
    draftConfig.minOrder(audience, value == null ? -1 : value);
  }

  @Command("towers draft order [order]")
  @CommandDescription("Set draft order")
  public void draftOrder(Audience audience, CommandSender sender, @Argument("order") String order) {
    draftConfig.order(audience, order);
  }

  @Command("towers draft private [value]")
  @CommandDescription("Set draft private match")
  public void draftPrivate(
      Audience audience, CommandSender sender, @Argument("value") Boolean value) {
    draftConfig.privateMatch(audience, value != null && value);
  }

  @Command("towers draft reroll [value]")
  @CommandDescription("Toggle draft reroll")
  public void draftReroll(
      Audience audience, CommandSender sender, @Argument("value") Boolean value) {
    draftConfig.reroll(audience, value);
  }

  @Command("towers draft secondpick [value]")
  @CommandDescription("Toggle draft second pick")
  public void draftSecondPick(
      Audience audience, CommandSender sender, @Argument("value") Boolean value) {
    draftConfig.secondPick(audience, value);
  }

  @Command("towers draft suggestions [value]")
  @CommandDescription("Toggle draft suggestions")
  public void draftSuggestions(
      Audience audience, CommandSender sender, @Argument("value") Boolean value) {
    draftConfig.suggestions(audience, value);
  }

  @Command("towers draft timer [value]")
  @CommandDescription("Toggle draft timer")
  public void draftTimer(
      Audience audience, CommandSender sender, @Argument("value") Boolean value) {
    draftConfig.timer(audience, value);
  }
}
