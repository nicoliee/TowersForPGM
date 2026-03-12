package org.nicolie.towersforpgm.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.commands.commandUtils.DraftConfig;
import org.nicolie.towersforpgm.commands.commandUtils.MatchBotConfigs;
import org.nicolie.towersforpgm.commands.commandUtils.PreparationConfig;
import org.nicolie.towersforpgm.commands.commandUtils.RankedConfig;
import org.nicolie.towersforpgm.commands.commandUtils.RefillConfig;
import org.nicolie.towersforpgm.commands.commandUtils.StatsConfig;
import org.nicolie.towersforpgm.configs.tables.TableType;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.util.Audience;

// Comando para configurar la mayoría de los aspectos del plugin, pensado para ser usado por
// administradores
// Este comando es el principal del plugin y permite configurar aspectos como el draft, la
// preparación, los stats y los refill

public class TowersCommand implements CommandExecutor, TabCompleter {
  private final DraftConfig draftConfig;
  private final StatsConfig statsConfig;
  private final PreparationConfig preparationConfig;
  private final RefillConfig refillConfig;
  private final RankedConfig rankedConfig;
  private final MatchBotConfigs matchBotConfig;
  List<String> MAIN_ARGS =
      Arrays.asList("draft", "matchbot", "preparation", "ranked", "refill", "rollback", "stats");
  List<String> DRAFT_ARGS =
      Arrays.asList("min", "order", "private", "reroll", "secondpick", "suggestions", "timer");
  List<String> MATCHBOT_ARGS = Arrays.asList(
      "accounts", "channel", "config", "rankedrole", "roles", "stats", "toggle", "voice");
  List<String> MATCHBOT_TABLE_ARGS = Arrays.asList("add", "remove", "list");
  List<String> MATCHBOT_VOICE_ARGS =
      Arrays.asList("inactive", "private", "queue", "reset", "toggle");
  List<String> PREPARATION_ARGS =
      Arrays.asList("add", "haste", "list", "max", "min", "remove", "timer", "toggle");
  List<String> RANKED_ARGS = Arrays.asList(
      "addmap",
      "maxsize",
      "minsize",
      "matchmaking",
      "order",
      "profile",
      "reroll",
      "setpool",
      "removemap");
  List<String> REFILL_ARGS = Arrays.asList("add", "delete", "reload");
  List<String> STATS_ARGS = Arrays.asList(
      "add", "addMap", "addTemporary", "default", "list", "remove", "removeMap", "removeTemporary");
  List<String> MATCHBOT_ROLES_ARGS =
      Arrays.asList("registered", "bronze", "silver", "gold", "emerald", "diamond");

  public TowersCommand() {
    this.draftConfig = new DraftConfig();
    this.statsConfig = new StatsConfig();
    this.preparationConfig = new PreparationConfig();
    this.refillConfig = new RefillConfig();
    this.rankedConfig = new RankedConfig();
    this.matchBotConfig = new MatchBotConfigs();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Audience audience = Audience.get(sender);
    if (args.length == 0) {
      audience.sendWarning(Component.translatable(
          "command.incorrectUsage", Component.text("/towers " + getArgsFormatted(MAIN_ARGS))));
      return true;
    }
    String mainArg = args[0].toLowerCase();

    switch (mainArg) {
      case "draft":
        if (args.length == 1) {
          audience.sendWarning(Component.translatable(
              "command.incorrectUsage",
              Component.text("/towers draft " + getArgsFormatted(DRAFT_ARGS))));
          return true;
        }

        switch (args[1].toLowerCase()) {
          case "min":
            int minArg = -1;
            try {
              if (args.length >= 3) {
                minArg = Integer.parseInt(args[2]);
              }
            } catch (NumberFormatException e) {
              audience.sendWarning(
                  Component.translatable("command.numberExpected", Component.text(args[2])));
              return true;
            }
            draftConfig.minOrder(audience, minArg);
            break;

          case "order":
            String order = args.length >= 3 ? args[2] : null;
            draftConfig.order(audience, order);
            break;

          case "private":
            Boolean privateArg =
                args.length >= 3 && parseBoolean(args[2]) != null ? parseBoolean(args[2]) : false;
            draftConfig.privateMatch(audience, privateArg);
            break;

          case "reroll":
            Boolean rerollValue = args.length >= 3 ? parseBoolean(args[2]) : null;
            draftConfig.reroll(audience, rerollValue);
            break;

          case "secondpick":
            Boolean secondPickValue = args.length >= 3 ? parseBoolean(args[2]) : null;
            draftConfig.secondPick(audience, secondPickValue);
            break;

          case "suggestions":
            Boolean suggestionsValue = args.length >= 3 ? parseBoolean(args[2]) : null;
            draftConfig.suggestions(audience, suggestionsValue);
            break;

          case "timer":
            Boolean timerValue = args.length >= 3 ? parseBoolean(args[2]) : null;
            draftConfig.timer(audience, timerValue);
            break;

          default:
            audience.sendWarning(Component.translatable(
                "command.incorrectUsage",
                Component.text("/towers draft " + getArgsFormatted(DRAFT_ARGS))));
            break;
        }
        break;

      case "preparation":
        if (args.length == 1) {
          audience.sendWarning(Component.translatable(
              "command.incorrectUsage",
              Component.text("/towers preparation " + getArgsFormatted(PREPARATION_ARGS))));
          return true;
        }

        switch (args[1].toLowerCase()) {
          case "add":
            preparationConfig.add(audience);
            break;

          case "haste":
            int haste = -1;
            if (args.length >= 3) {
              try {
                haste = Integer.parseInt(args[2]);
              } catch (NumberFormatException ignored) {
                audience.sendWarning(Component.text("/towers preparation haste <level>"));
              }
            }
            preparationConfig.haste(audience, haste);
            return true;

          case "list":
            preparationConfig.list(audience);
            return true;

          case "max":
            if (args.length < 5) {
              audience.sendWarning(Component.text("/towers preparation max <x> <y> <z>"));
              return true;
            }
            try {
              int x = Integer.parseInt(args[2]);
              int y = Integer.parseInt(args[3]);
              int z = Integer.parseInt(args[4]);
              preparationConfig.max(audience, x + "," + y + "," + z);
            } catch (NumberFormatException e) {
              audience.sendWarning(Component.text("/towers preparation max <x> <y> <z>"));
            }
            break;

          case "min":
            if (args.length < 5) {
              audience.sendWarning(Component.text("/towers preparation min <x> <y> <z>"));
              return true;
            }
            try {
              int x = Integer.parseInt(args[2]);
              int y = Integer.parseInt(args[3]);
              int z = Integer.parseInt(args[4]);
              preparationConfig.min(audience, x + "," + y + "," + z);
            } catch (NumberFormatException e) {
              audience.sendWarning(Component.text("/towers preparation min <x> <y> <z>"));
            }
            break;

          case "remove":
            preparationConfig.remove(audience);
            break;

          case "timer":
            int timer = -1;
            if (args.length >= 3) {
              try {
                timer = Integer.parseInt(args[2]);
              } catch (NumberFormatException ignored) {
                audience.sendWarning(Component.text("/towers preparation timer <mins>"));
              }
            }
            preparationConfig.timer(audience, timer);
            return true;

          case "toggle":
            Boolean toggleValue = args.length >= 3 ? parseBoolean(args[2]) : null;
            preparationConfig.enabled(audience, toggleValue);
            break;

          default:
            audience.sendWarning(Component.translatable(
                "command.incorrectUsage",
                Component.text("/towers preparation " + getArgsFormatted(PREPARATION_ARGS))));
            break;
        }
        break;
      case "ranked":
        if (args.length == 1) {
          audience.sendWarning(Component.translatable(
              "command.incorrectUsage",
              Component.text("/towers ranked " + getArgsFormatted(RANKED_ARGS))));
          return true;
        }
        switch (args[1].toLowerCase()) {
          case "addtable":
            String addTableArg = args.length >= 3 ? args[2] : null;
            rankedConfig.tableAdd(audience, addTableArg);
            return true;

          case "matchmaking":
            Boolean matchmakingArg = null;
            if (args.length >= 3) {
              matchmakingArg = parseBoolean(args[2]);
            }
            rankedConfig.matchmaking(audience, matchmakingArg);
            return true;

          case "maxsize":
            String maxSizeArg = args.length >= 3 ? args[2] : null;
            rankedConfig.maxSize(audience, maxSizeArg);
            return true;

          case "minsize":
            String minSizeArg = args.length >= 3 ? args[2] : null;
            rankedConfig.minSize(audience, minSizeArg);
            return true;

          case "order":
            String orderArg = args.length >= 3 ? args[2] : null;
            rankedConfig.order(audience, orderArg);
            return true;

          case "profile":
            String profileArg = args.length >= 3 ? args[2] : null;
            rankedConfig.profile(audience, profileArg);
            return true;

          case "removetable":
            String removeTableArg = args.length >= 3 ? args[2] : null;
            rankedConfig.tableRemove(audience, removeTableArg);
            return true;

          case "reroll":
            Boolean newRerollValue = null;

            if (args.length >= 3) {
              newRerollValue = Boolean.parseBoolean(args[2]);
            }
            rankedConfig.reroll(audience, newRerollValue);
            break;

          case "setpool":
            String poolArg = args.length >= 3 ? args[2] : null;
            rankedConfig.pool(audience, poolArg);
            return true;

          default:
            audience.sendWarning(Component.translatable(
                "command.incorrectUsage",
                Component.text("/towers ranked " + getArgsFormatted(RANKED_ARGS))));
        }
        break;
      case "refill":
        if (args.length == 1) {
          audience.sendWarning(Component.translatable(
              "command.incorrectUsage",
              Component.text("/towers refill " + getArgsFormatted(REFILL_ARGS))));
          return true;
        }
        Location loc = ((Player) sender).getLocation();
        String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
        switch (args[1].toLowerCase()) {
          case "add":
            refillConfig.chest(audience, mapName, loc);
            return true;

          case "delete":
            refillConfig.remove(audience, mapName, loc);
            return true;

          case "reload":
            String worldName = ((Player) sender).getWorld().getName();
            refillConfig.test(audience, mapName, worldName);
            return true;

          default:
            audience.sendWarning(Component.translatable(
                "command.incorrectUsage",
                Component.text("/towers refill " + getArgsFormatted(REFILL_ARGS))));
            return true;
        }

      case "stats":
        if (args.length == 1) {
          audience.sendWarning(Component.translatable(
              "command.incorrectUsage",
              Component.text("/towers stats " + getArgsFormatted(STATS_ARGS))));
          return true;
        }

        switch (args[1].toLowerCase()) {
          case "add":
            if (args.length < 3) {
              audience.sendWarning(Component.translatable(
                  "command.incorrectUsage", Component.text("/towers stats add <tabla>")));
              return true;
            }
            String newTableName = args[2];
            statsConfig.tableAdd(audience, newTableName);
            break;

          case "addmap":
            if (args.length < 3) {
              audience.sendWarning(Component.translatable(
                  "command.incorrectUsage", Component.text("/towers stats addMap <tabla>")));
              return true;
            }
            String addMapTableName = args[2];
            statsConfig.map(audience, addMapTableName);
            return true;

          case "addtemporary":
            if (args.length < 3) {
              audience.sendWarning(Component.translatable(
                  "command.incorrectUsage", Component.text("/towers stats addTemporary <tabla>")));
              return true;
            }
            String addTemporaryTableName = args[2];
            statsConfig.temp(audience, addTemporaryTableName);
            return true;

          case "default":
            String tableName = args.length >= 3 ? args[2] : null;
            statsConfig.defaultTable(audience, tableName);
            break;

          case "list":
            statsConfig.tables(audience);
            return true;

          case "remove":
            if (args.length < 3) {
              audience.sendWarning(Component.translatable(
                  "command.incorrectUsage", Component.text("/towers stats remove <tabla>")));
              return true;
            }
            String removeTableName = args[2];
            statsConfig.tableRemove(audience, removeTableName);
            return true;

          case "removemap":
            statsConfig.mapRemove(audience);
            return true;

          case "removetemporary":
            statsConfig.tempRemove(audience);
            return true;

          default:
            audience.sendWarning(Component.translatable(
                "command.incorrectUsage",
                Component.text("/towers stats " + getArgsFormatted(STATS_ARGS))));
            return true;
        }
        break;

      case "rollback":
        if (args.length < 2) {
          audience.sendWarning(Component.translatable(
              "command.incorrectUsage", Component.text("/towers rollback <matchID>")));
          return true;
        }
        String matchId = args[1];
        // Ejecutar rollback asincrónico
        org.nicolie.towersforpgm.database.MatchHistoryManager.getMatch(matchId)
            .thenAccept(history -> {
              if (history == null) {
                org.bukkit.Bukkit.getScheduler().runTask(TowersForPGM.getInstance(), () -> {
                  audience.sendMessage(
                      Component.translatable("rollback.matchNotFound", Component.text(matchId)));
                });
                return;
              }
              org.nicolie.towersforpgm.database.MatchHistoryManager.rollbackMatch(
                  audience, history);
            });
        return true;

      case "matchbot":
        if (!TowersForPGM.getInstance().isMatchBotEnabled()) {
          audience.sendWarning(Component.translatable("matchbot.notEnabled"));
          return true;
        }

        if (args.length == 1) {
          audience.sendWarning(Component.translatable(
              "command.incorrectUsage",
              Component.text("/towers matchbot " + getArgsFormatted(MATCHBOT_ARGS))));
          return true;
        }
        switch (args[1].toLowerCase()) {
          case "accounts":
            String accountsTable = args.length >= 3 ? args[2] : null;
            matchBotConfig.accounts(audience, accountsTable);
            break;

          case "channel":
            String valueStr = args.length >= 3 ? args[2] : null;
            matchBotConfig.discordChannel(audience, valueStr);
            break;

          case "config":
            matchBotConfig.showConfig(audience);
            break;

          case "rankedrole":
            String rankedRoleId = args.length >= 3 ? args[2] : null;
            matchBotConfig.rankedRole(audience, rankedRoleId);
            break;

          case "roles":
            if (args.length < 3) {
              audience.sendWarning(Component.text(
                  "/towers matchbot roles " + getArgsFormatted(MATCHBOT_ROLES_ARGS)));
              return true;
            }
            switch (args[2].toLowerCase()) {
              case "bronze":
                String bronzeRoleId = args.length >= 4 ? args[3] : null;
                matchBotConfig.bronzeRole(audience, bronzeRoleId);
                break;
              case "diamond":
                String diamondRoleId = args.length >= 4 ? args[3] : null;
                matchBotConfig.diamondRole(audience, diamondRoleId);
                break;
              case "emerald":
                String emeraldRoleId = args.length >= 4 ? args[3] : null;
                matchBotConfig.emeraldRole(audience, emeraldRoleId);
                break;
              case "gold":
                String goldRoleId = args.length >= 4 ? args[3] : null;
                matchBotConfig.goldRole(audience, goldRoleId);
                break;
              case "registered":
                String registeredRoleId = args.length >= 4 ? args[3] : null;
                matchBotConfig.registeredRole(audience, registeredRoleId);
                break;
              case "silver":
                String silverRoleId = args.length >= 4 ? args[3] : null;
                matchBotConfig.silverRole(audience, silverRoleId);
                break;
              default:
                audience.sendWarning(Component.text(
                    "/towers matchbot roles " + getArgsFormatted(MATCHBOT_ROLES_ARGS)));
                break;
            }
            break;

          case "stats":
            Boolean value = args.length >= 3 ? parseBoolean(args[2]) : null;
            matchBotConfig.statPoints(audience, value);
            break;

          case "tables":
            if (args.length < 3) {
              audience.sendWarning(Component.translatable(
                  "command.incorrectUsage",
                  Component.text(
                      "/towers matchbot tables " + getArgsFormatted(MATCHBOT_TABLE_ARGS))));
              return true;
            }
            switch (args[2].toLowerCase()) {
              case "add":
                if (args.length < 4) {
                  audience.sendWarning(Component.translatable(
                      "command.incorrectUsage",
                      Component.text("/towers matchbot tables add <table>")));
                  return true;
                }
                matchBotConfig.addTable(audience, args[3]);
                break;
              case "list":
                matchBotConfig.listTables(audience);
                break;
              case "remove":
                if (args.length < 4) {
                  audience.sendWarning(Component.translatable(
                      "command.incorrectUsage",
                      Component.text("/towers matchbot tables remove <table>")));
                  return true;
                }
                matchBotConfig.removeTable(audience, args[3]);
                break;
              default:
                audience.sendWarning(Component.translatable(
                    "command.incorrectUsage",
                    Component.text(
                        "/towers matchbot tables " + getArgsFormatted(MATCHBOT_TABLE_ARGS))));
                break;
            }
            break;

          case "toggle":
            Boolean toggleValue = args.length >= 3 ? parseBoolean(args[2]) : null;
            matchBotConfig.toggle(audience, toggleValue);
            break;

          case "voice":
            if (args.length < 3) {
              audience.sendWarning(Component.text(
                  "/towers matchbot voice " + getArgsFormatted(MATCHBOT_VOICE_ARGS)));
              break;
            }
            switch (args[2].toLowerCase()) {
              case "inactive":
                String inactiveChannelId = args.length >= 4 ? args[3] : null;
                matchBotConfig.voiceChannelInactive(audience, inactiveChannelId);
                break;
              case "private":
                Boolean privateChannelsEnabled = args.length >= 4 ? parseBoolean(args[3]) : null;
                matchBotConfig.privateChannels(audience, privateChannelsEnabled);
                break;
              case "queue":
                String queueChannelId = args.length >= 4 ? args[3] : null;
                matchBotConfig.voiceChannelQueue(audience, queueChannelId);
                break;
              case "reset":
                matchBotConfig.channelsRemove(audience);
                break;
              case "toggle":
                Boolean voiceChatEnabled = args.length >= 4 ? parseBoolean(args[3]) : null;
                matchBotConfig.voiceChat(audience, voiceChatEnabled);
                break;
              default:
                audience.sendWarning(Component.text(
                    "/towers matchbot voice " + getArgsFormatted(MATCHBOT_VOICE_ARGS)));
                break;
            }
            break;

          default:
            audience.sendWarning(Component.translatable(
                "command.incorrectUsage",
                Component.text("/towers matchbot " + getArgsFormatted(MATCHBOT_ARGS))));
            break;
        }
        break;
      default:
        audience.sendWarning(Component.translatable(
            "command.incorrectUsage", Component.text("/towers " + getArgsFormatted(MAIN_ARGS))));
        return true;
    }

    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      List<String> base = MAIN_ARGS;
      String input = args[0].toLowerCase();
      return base.stream().filter(s -> s.startsWith(input)).collect(Collectors.toList());
    }

    String first = args[0].toLowerCase();

    switch (first) {
      case "draft":
        if (args.length == 2) {
          List<String> options = DRAFT_ARGS;
          return filterPrefix(options, args[1]);
        }
        if (args.length == 3) {
          String sub = args[1].toLowerCase();
          if (Arrays.asList("private", "secondpickbalance", "suggestions", "matchmaking", "timer")
              .contains(sub)) {
            return filterPrefix(Arrays.asList("true", "false"), args[2]);
          }
        }
        break;
      case "rollback":
        if (args.length == 2) {
          // Sin autocomplete por el momento
          return new java.util.ArrayList<>();
        }
        break;

      case "preparation":
        if (args.length == 2) {
          List<String> options = PREPARATION_ARGS;
          return filterPrefix(options, args[1]);
        }
        if (args.length >= 3) {
          String sub = args[1].toLowerCase();
          if ((sub.equals("max") || sub.equals("min"))) {
            return suggestNumbers(args.length);
          } else if (sub.equals("timer") || sub.equals("haste")) {
            return suggestNumbers(args.length);
          }
        }
        break;
      case "ranked":
        if (args.length == 2) {
          List<String> options = RANKED_ARGS;
          return filterPrefix(options, args[1]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("profile")) {
          // Autocompletar nombres de perfiles
          Set<String> profiles = TowersForPGM.getInstance().config().ranked().getProfileNames();
          return filterPrefix(profiles, args[2]);
        }
      case "refill":
        if (args.length == 2) {
          List<String> options = Arrays.asList("add", "delete", "reload");
          return filterPrefix(options, args[1]);
        }
        break;

      case "stats":
        if (args.length == 2) {
          List<String> options = STATS_ARGS;
          return filterPrefix(options, args[1]);
        }
        if (args.length == 3) {
          String sub = args[1].toLowerCase();
          if (Arrays.asList("default", "add", "remove", "addmap", "addtemporary")
              .contains(sub)) {
            Set<String> tables =
                TowersForPGM.getInstance().config().databaseTables().getTables(TableType.ALL);
            return filterPrefix(tables, args[2]);
          }
        }
        break;

      case "matchbot":
        if (args.length == 2) {
          List<String> options = MATCHBOT_ARGS;
          return filterPrefix(options, args[1]);
        }
        if (args.length == 3) {
          String sub = args[1].toLowerCase();
          if (sub.equals("voice")) {
            List<String> voiceOptions = MATCHBOT_VOICE_ARGS;
            return filterPrefix(voiceOptions, args[2]);
          } else if (sub.equals("roles")) {
            List<String> roleOptions = MATCHBOT_ROLES_ARGS;
            return filterPrefix(roleOptions, args[2]);
          } else if (sub.equals("tables")) {
            List<String> tableOptions = Arrays.asList("add", "remove", "list");
            return filterPrefix(tableOptions, args[2]);
          } else if (sub.equals("accounts")) {
            Set<String> tables =
                TowersForPGM.getInstance().config().databaseTables().getTables(TableType.ALL);
            return filterPrefix(tables, args[2]);
          }
        }
        if (args.length == 4) {
          String sub = args[1].toLowerCase();
          String subsub = args[2].toLowerCase();
          if (sub.equals("tables") && (subsub.equals("add") || subsub.equals("remove"))) {
            Set<String> tables =
                TowersForPGM.getInstance().config().databaseTables().getTables(TableType.ALL);
            return filterPrefix(tables, args[3]);
          }
        }
        break;
    }

    return null;
  }

  private Boolean parseBoolean(String value) {
    if (value.equalsIgnoreCase("true")) {
      return true;
    } else if (value.equalsIgnoreCase("false")) {
      return false;
    }
    return null;
  }

  private List<String> filterPrefix(List<String> options, String input) {
    return options.stream()
        .filter(option -> option.toLowerCase().startsWith(input.toLowerCase()))
        .collect(Collectors.toList());
  }

  private List<String> filterPrefix(Set<String> options, String input) {
    return options.stream()
        .filter(option -> option.toLowerCase().startsWith(input.toLowerCase()))
        .collect(Collectors.toList());
  }

  private List<String> suggestNumbers(int argPosition) {
    List<String> numbers = Arrays.asList("0", "10", "25", "50", "100");
    return numbers;
  }

  private String getArgsFormatted(List<String> args) {
    if (args == null || args.isEmpty()) {
      return "<>";
    }
    String formatted = args.stream().map(arg -> arg).collect(Collectors.joining("|"));
    return "<" + formatted + ">";
  }
}
