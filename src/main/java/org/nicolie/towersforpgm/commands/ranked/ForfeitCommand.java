package org.nicolie.towersforpgm.commands.ranked;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import me.tbg.match.bot.configs.DiscordBot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.tables.TableInfo;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.draft.team.Teams;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.embeds.Forfeit;
import org.nicolie.towersforpgm.rankeds.DisconnectManager;
import org.nicolie.towersforpgm.rankeds.Elo;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.rankeds.Queue;
import org.nicolie.towersforpgm.session.MatchSession;
import org.nicolie.towersforpgm.session.MatchSessionRegistry;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.util.bukkit.Sounds;

public class ForfeitCommand {
  @Command("forfeit|ff")
  @CommandDescription("Forfeit the current ranked match")
  public void forfeitCommand(Player sender) {
    Match match = PGM.get().getMatchManager().getMatch(sender);
    if (match == null) return;

    MatchPlayer matchPlayer = match.getPlayer(sender);
    if (matchPlayer == null) return;

    if (!Queue.isRanked() || matchPlayer.isObserving()) {
      matchPlayer.sendWarning(Component.translatable("ranked.noForfeit"));
      return;
    }

    Party team = matchPlayer.getParty();

    // Already voted
    if (!DisconnectManager.addForfeit(match, matchPlayer)) {
      matchPlayer.sendWarning(Component.translatable("ranked.alreadyForfeited"));
      return;
    }

    match.sendMessage(Queue.RANKED_PREFIX
        .append(Component.space())
        .append(Component.translatable("ranked.forfeit", matchPlayer.getName())));
    match.playSound(Sounds.ALERT);

    if (!DisconnectManager.allForfeited(match, team)) return;

    // All players on the team have voted — resolve outcome
    Teams teams = teamsForMatch(match);
    int currentTeamNumber = teams != null ? teams.getTeamNumber(team) : -1;
    int opponentTeamNumber = currentTeamNumber == 1 ? 2 : 1;
    String winningTeam = teams != null ? teams.getTeamName(opponentTeamNumber) : "";

    boolean sanctionedForThisTeam = DisconnectManager.isSanctionActive(match)
        && DisconnectManager.isSanctionForTeam(match, team);

    if (sanctionedForThisTeam) {
      handleSanctionedForfeit(match, team, teams);
    } else {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "end " + winningTeam);
    }

    DisconnectManager.clearMatch(match);
  }

  private void handleSanctionedForfeit(Match match, Party losingTeam, Teams teams) {
    String sanctionedUser = DisconnectManager.getSanctionedUsername(match);

    if (sanctionedUser == null) {
      TowersForPGM.getInstance().setStatsCancel(true);
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "end");
      return;
    }

    List<MatchPlayer> sanctionedTeam = new ArrayList<>(losingTeam.getPlayers());
    List<MatchPlayer> opponentTeam = new ArrayList<>();
    for (Party p : match.getParties()) {
      if (!p.equals(losingTeam)) opponentTeam.addAll(p.getPlayers());
    }

    Elo.doubleLossPenalty(sanctionedUser, sanctionedTeam, opponentTeam)
        .thenAccept((PlayerEloChange penalty) -> Bukkit.getScheduler()
            .runTask(TowersForPGM.getInstance(), () -> {
              TowersForPGM.getInstance().setStatsCancel(true);

              String table = TowersForPGM.getInstance()
                  .config()
                  .databaseTables()
                  .getTable(match.getMap().getName());
              StatsManager.applySanction(table, sanctionedUser, 2, penalty);

              match.sendMessage(Queue.RANKED_PREFIX
                  .append(Component.space())
                  .append(Component.translatable("ranked.forfeitSanctionApplied")
                      .color(NamedTextColor.RED)
                      .arguments(
                          Component.text(sanctionedUser).color(NamedTextColor.GRAY),
                          Component.text("-" + Math.abs(penalty.getEloChange()))
                              .color(NamedTextColor.GRAY))));

              sendMatchBotEmbed(match, table, sanctionedUser, losingTeam, penalty);

              Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "end");
            }));
  }

  private void sendMatchBotEmbed(
      Match match, String table, String sanctionedUser, Party losingTeam, PlayerEloChange penalty) {
    if (!TowersForPGM.getInstance().isMatchBotEnabled()) return;

    TableInfo tableInfo = TowersForPGM.getInstance().config().databaseTables().getTableInfo(table);
    if (tableInfo == null || !tableInfo.isRanked()) return;

    java.util.Set<String> usernames = new HashSet<>();
    for (MatchPlayer mp : match.getPlayers()) usernames.add(mp.getNameLegacy());
    if (!sanctionedUser.isEmpty()) usernames.add(sanctionedUser);

    StatsManager.getEloForUsernames(table, new ArrayList<>(usernames)).thenAccept(eloList -> {
      net.dv8tion.jda.api.EmbedBuilder embed = Forfeit.create(
          match,
          table,
          eloList,
          sanctionedUser,
          losingTeam.getNameLegacy(),
          penalty.getEloChange());
      DiscordBot.sendMatchEmbed(
          embed,
          MatchBotConfig.getDiscordChannel(),
          null,
          DiscordBot.setEmbedThumbnail(match.getMap(), embed));
    });
  }

  private static Teams teamsForMatch(Match match) {
    if (match == null) return null;
    MatchSession session = MatchSessionRegistry.get(match);
    if (session == null) return null;
    return session.teams();
  }
}
