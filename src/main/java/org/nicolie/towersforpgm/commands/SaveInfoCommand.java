package org.nicolie.towersforpgm.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.session.bridge.CrossMatchBridge;
import org.nicolie.towersforpgm.session.bridge.CrossMatchSnapshot;
import org.nicolie.towersforpgm.utils.Permissions;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.util.Audience;

public class SaveInfoCommand {

  @Command("saveInfo")
  @CommandDescription("Save the current match state so it survives the next map cycle")
  @Permission(Permissions.ADMIN)
  public void saveInfo(Audience audience, Player sender) {
    Match match = PGM.get().getMatchManager().getMatch(sender);
    if (match == null) {
      audience.sendWarning(Component.text("No match found.", NamedTextColor.RED));
      return;
    }

    CrossMatchBridge.CaptureResult result = CrossMatchBridge.getInstance().capture(match);

    switch (result) {
      case OK:
        CrossMatchSnapshot snapshot = CrossMatchBridge.getInstance().getPending();
        audience.sendMessage(Component.text("✔ Match state saved.", NamedTextColor.GREEN)
            .append(Component.newline())
            .append(info("Phase", snapshot.getDraftPhase().name()))
            .append(Component.newline())
            .append(info("Picks", snapshot.getPickHistory().size()))
            .append(Component.newline())
            .append(info("Pool", snapshot.getRemainingPool().size()))
            .append(Component.newline())
            .append(info("Ranked", snapshot.isRanked()))
            .append(Component.newline())
            .append(info("Ranked players", snapshot.getRankedPlayers().size()))
            .append(Component.newline())
            .append(Component.text(
                "Will be restored automatically on the next map load.", NamedTextColor.AQUA)));
        break;

      case NOTHING_TO_SAVE:
        audience.sendWarning(Component.text(
            "Nothing to save — no active draft or ranked session.", NamedTextColor.YELLOW));
        break;

      case NO_SESSION:
        audience.sendWarning(
            Component.text("No session found for this match.", NamedTextColor.RED));
        break;
    }
  }

  @Command("saveInfo clear")
  @CommandDescription("Discard the pending cross-match snapshot")
  @Permission(Permissions.ADMIN)
  public void clearSaveInfo(Audience audience) {
    if (!CrossMatchBridge.getInstance().hasPending()) {
      audience.sendWarning(Component.text("No pending snapshot to clear.", NamedTextColor.YELLOW));
      return;
    }
    CrossMatchBridge.getInstance().clear();
    audience.sendMessage(Component.text("✔ Pending snapshot cleared.", NamedTextColor.GREEN));
  }

  @Command("saveInfo status")
  @CommandDescription("Show the current pending snapshot, if any")
  @Permission(Permissions.ADMIN)
  public void statusSaveInfo(Audience audience) {
    CrossMatchSnapshot s = CrossMatchBridge.getInstance().getPending();
    if (s == null) {
      audience.sendMessage(Component.text("No pending snapshot.", NamedTextColor.GRAY));
      return;
    }

    audience.sendMessage(Component.text("Pending snapshot:", NamedTextColor.GOLD)
        .append(Component.newline())
        .append(info("Phase", s.getDraftPhase().name()))
        .append(Component.newline())
        .append(info("Captain 1", s.getCaptain1()))
        .append(Component.newline())
        .append(info("Captain 2", s.getCaptain2()))
        .append(Component.newline())
        .append(info("Picks", s.getPickHistory().size()))
        .append(Component.newline())
        .append(info("Pool", s.getRemainingPool().size()))
        .append(Component.newline())
        .append(info("Pattern", s.getOrderPattern()))
        .append(Component.newline())
        .append(info("Pattern index", s.getPatternIndex()))
        .append(Component.newline())
        .append(info("Ranked", s.isRanked()))
        .append(Component.newline())
        .append(info("Ranked players", s.getRankedPlayers().size())));
  }

  private static Component info(String label, Object value) {
    return Component.text("  " + label + ": ", NamedTextColor.GRAY)
        .append(Component.text(String.valueOf(value), NamedTextColor.WHITE));
  }
}
