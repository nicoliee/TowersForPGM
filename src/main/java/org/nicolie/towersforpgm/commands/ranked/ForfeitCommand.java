package org.nicolie.towersforpgm.commands.ranked;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.rankeds.disconnect.ForfeitManager;
import org.nicolie.towersforpgm.rankeds.disconnect.ForfeitManager.ForfeitResult;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;

public class ForfeitCommand {

  @Command("forfeit|ff")
  @CommandDescription("Forfeit the current ranked match")
  public void forfeitCommand(Player sender) {
    Match match = PGM.get().getMatchManager().getMatch(sender);
    if (match == null) return;

    MatchPlayer matchPlayer = match.getPlayer(sender);
    if (matchPlayer == null) return;

    ForfeitResult result = ForfeitManager.vote(match, matchPlayer);

    if (result == ForfeitResult.NOT_RANKED) {
      matchPlayer.sendWarning(Component.translatable("ranked.noForfeit"));
    } else if (result == ForfeitResult.ALREADY_VOTED) {
      matchPlayer.sendWarning(Component.translatable("ranked.alreadyForfeited"));
    }
  }
}
