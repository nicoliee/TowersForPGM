package org.nicolie.towersforpgm.commands.ranked;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import me.tbg.match.bot.configs.DiscordBot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.configs.tables.TableInfo;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.embeds.Forfeit;
import org.nicolie.towersforpgm.rankeds.DisconnectManager;
import org.nicolie.towersforpgm.rankeds.Elo;
import org.nicolie.towersforpgm.rankeds.PlayerEloChange;
import org.nicolie.towersforpgm.rankeds.Queue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.bukkit.Sounds;

public class ForfeitCommand implements CommandExecutor {
  private final TowersForPGM plugin = TowersForPGM.getInstance();
  private final Teams teams;
  public static Set<UUID> forfeitedPlayers = new HashSet<>();

  public ForfeitCommand(Teams teams) {
    this.teams = teams;
  }

  public static void resetTeamForfeits(Party team) {
    if (team == null) return;
    team.getPlayers().forEach(mp -> forfeitedPlayers.remove(mp.getId()));
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Audience audience = Audience.get(sender);
    if (!(sender instanceof Player)) {
      audience.sendWarning(Component.translatable("command.onlyPlayers"));
      return true;
    }
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer((Player) sender);
    if (!Queue.isRanked() || matchPlayer.isObserving()) {
      matchPlayer.sendWarning(Component.translatable("ranked.noForfeit"));
      return true;
    }
    Match match = PGM.get().getMatchManager().getMatch(sender);
    Party team = matchPlayer.getParty();

    if (!forfeitedPlayers.add(matchPlayer.getId())) {
      matchPlayer.sendWarning(Component.translatable("ranked.alreadyForfeited"));
      return true;
    }
    match.sendMessage(Queue.RANKED_PREFIX
        .append(Component.space())
        .append(Component.translatable("ranked.forfeit", matchPlayer.getName())));
    match.playSound(Sounds.ALERT);

    boolean allForfeited = team.getPlayers().stream()
        .filter(mp -> !DisconnectManager.isSanctionedPlayer(match, mp))
        .allMatch(mp -> forfeitedPlayers.contains(mp.getId()));

    // Get the opposing team name
    int currentTeamNumber = teams.getTeamNumber(team);
    int opponentTeamNumber = currentTeamNumber == 1 ? 2 : 1;
    String winningTeam = teams.getTeamName(opponentTeamNumber);
    if (allForfeited) {
      boolean sanctionedForThisTeam = DisconnectManager.isSanctionActive(match)
          && DisconnectManager.isSanctionForTeam(match, team);

      if (sanctionedForThisTeam) {
        String sanctionedUser = DisconnectManager.getSanctionedUsername(match);

        if (sanctionedUser != null) {
          java.util.List<MatchPlayer> sanctionedTeam = new java.util.ArrayList<>(team.getPlayers());
          java.util.List<MatchPlayer> opponentTeam = new java.util.ArrayList<>();
          for (tc.oc.pgm.api.party.Party p : match.getParties()) {
            if (!p.equals(team)) opponentTeam.addAll(p.getPlayers());
          }

          Elo.doubleLossPenalty(sanctionedUser, sanctionedTeam, opponentTeam)
              .thenAccept((PlayerEloChange penalty) -> {
                Bukkit.getScheduler().runTask(TowersForPGM.getInstance(), () -> {
                  TowersForPGM.getInstance().setStatsCancel(true);
                  String table =
                      plugin.config().databaseTables().getTable(match.getMap().getName());
                  StatsManager.applySanction(table, sanctionedUser, 2, penalty);
                  match.sendMessage(Queue.RANKED_PREFIX
                      .append(Component.space())
                      .append(Component.translatable("ranked.forfeitSanctionApplied")
                          .color(NamedTextColor.RED)
                          .arguments(
                              Component.text(sanctionedUser).color(NamedTextColor.GRAY),
                              Component.text("-" + String.valueOf(Math.abs(penalty.getEloChange())))
                                  .color(NamedTextColor.GRAY))));
                  TableInfo tableInfo =
                      TowersForPGM.getInstance().config().databaseTables().getTableInfo(table);
                  boolean isRankedTable = tableInfo != null && tableInfo.isRanked();
                  if (TowersForPGM.getInstance().isMatchBotEnabled() && isRankedTable) {
                    java.util.Set<String> usernames = new java.util.HashSet<>();
                    for (MatchPlayer mp : match.getPlayers()) usernames.add(mp.getNameLegacy());
                    if (sanctionedUser != null && !sanctionedUser.isEmpty())
                      usernames.add(sanctionedUser);

                    org.nicolie.towersforpgm.database.StatsManager.getEloForUsernames(
                            table, new java.util.ArrayList<>(usernames))
                        .thenAccept(eloList -> {
                          net.dv8tion.jda.api.EmbedBuilder embed = Forfeit.create(
                              match,
                              table,
                              eloList,
                              sanctionedUser,
                              team.getNameLegacy(),
                              penalty.getEloChange());
                          DiscordBot.sendMatchEmbed(
                              embed,
                              MatchBotConfig.getDiscordChannel(),
                              null,
                              DiscordBot.setEmbedThumbnail(match.getMap(), embed));
                        });
                  }

                  Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "end");
                });
              });
        } else {
          TowersForPGM.getInstance().setStatsCancel(true);
          Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "end");
        }
      } else {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "end " + winningTeam);
      }
      forfeitedPlayers.clear();
      DisconnectManager.clearMatch(match);
    }
    return true;
  }
}
