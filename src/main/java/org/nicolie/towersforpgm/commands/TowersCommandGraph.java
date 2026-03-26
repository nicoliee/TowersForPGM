package org.nicolie.towersforpgm.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.commands.draft.DraftCaptainCommands;
import org.nicolie.towersforpgm.commands.draft.DraftManagerCommands;
import org.nicolie.towersforpgm.commands.draft.DraftStartCommands;
import org.nicolie.towersforpgm.commands.history.HistoryCommand;
import org.nicolie.towersforpgm.commands.ranked.EloCommand;
import org.nicolie.towersforpgm.commands.ranked.ForfeitCommand;
import org.nicolie.towersforpgm.commands.ranked.LinkCommand;
import org.nicolie.towersforpgm.commands.ranked.RankedCommand;
import org.nicolie.towersforpgm.commands.ranked.TagCommand;
import org.nicolie.towersforpgm.commands.ranked.UnlinkCommand;
import org.nicolie.towersforpgm.commands.towers.DraftCommand;
import org.nicolie.towersforpgm.commands.towers.MatchBotCommand;
import org.nicolie.towersforpgm.commands.towers.PreparationCommand;
import org.nicolie.towersforpgm.commands.towers.RefillCommand;
import org.nicolie.towersforpgm.commands.towers.RollbackCommand;
import org.nicolie.towersforpgm.commands.towers.StatsCommand;
import org.nicolie.towersforpgm.rankeds.Queue;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.command.injectors.AudienceProvider;
import tc.oc.pgm.command.parsers.MapInfoParser;
import tc.oc.pgm.command.parsers.MapPoolParser;
import tc.oc.pgm.command.parsers.MatchPlayerParser;
import tc.oc.pgm.command.parsers.OfflinePlayerParser;
import tc.oc.pgm.command.parsers.PartyParser;
import tc.oc.pgm.command.parsers.PlayerParser;
import tc.oc.pgm.command.util.CommandGraph;
import tc.oc.pgm.lib.org.incendo.cloud.minecraft.extras.MinecraftHelp;
import tc.oc.pgm.rotation.pools.MapPool;
import tc.oc.pgm.util.Audience;

public class TowersCommandGraph extends CommandGraph<TowersForPGM> {

  @Override
  protected MinecraftHelp<CommandSender> createHelp() {
    return MinecraftHelp.create("/towersforpgm help", manager, Audience::get);
  }

  public TowersCommandGraph(TowersForPGM plugin) throws Exception {
    super(plugin);
  }

  @Override
  protected void setupInjectors() {
    registerInjector(Audience.class, new AudienceProvider());
  }

  @Override
  protected void setupParsers() {
    registerParser(Party.class, PartyParser::new);
    registerParser(MapInfo.class, MapInfoParser::new);
    registerParser(Player.class, new PlayerParser());
    registerParser(Party.class, PartyParser::new);
    registerParser(MatchPlayer.class, new MatchPlayerParser());
    registerParser(MapPool.class, new MapPoolParser());
    registerParser(OfflinePlayer.class, new OfflinePlayerParser());
  }

  @Override
  protected void registerCommands() {
    register(new SudoCommand());
    register(new DraftCaptainCommands());
    register(new DraftManagerCommands());
    register(new DraftStartCommands());
    register(new RankedCommand(Queue.getInstance()));
    register(new ForfeitCommand());
    register(new EloCommand());
    register(new LinkCommand());
    register(new TagCommand());
    register(new UnlinkCommand());
    register(new HistoryCommand(plugin));

    register(new DraftCommand());
    register(new PreparationCommand());
    register(new org.nicolie.towersforpgm.commands.towers.RankedCommand());
    register(new RefillCommand());
    register(new StatsCommand());
    register(new RollbackCommand());
    register(new MatchBotCommand());
    register(new SaveInfoCommand());

    registerHelpCommand();
  }

  private void registerHelpCommand() {
    manager.command(manager
        .commandBuilder("towersforpgm")
        .literal("help")
        .optional(
            "query",
            tc.oc.pgm.lib.org.incendo.cloud.parser.standard.StringParser.greedyStringParser())
        .handler(context -> minecraftHelp.queryCommands(
            context.<String>optional("query").orElse(""), context.sender())));
  }
}
