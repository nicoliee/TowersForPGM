package org.nicolie.towersforpgm.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.commandUtils.DraftConfig;
import org.nicolie.towersforpgm.commandUtils.HelpConfig;
import org.nicolie.towersforpgm.commandUtils.PreparationConfig;
import org.nicolie.towersforpgm.commandUtils.StatsConfig;
import org.nicolie.towersforpgm.refill.RefillManager;
import org.nicolie.towersforpgm.commandUtils.RefillConfig;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;

import tc.oc.pgm.api.PGM;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// Comando para configurar la mayoría de los aspectos del plugin, pensado para ser usado por administradores
// Este comando es el principal del plugin y permite configurar aspectos como el draft, la preparación, los stats y los refill

public class TowersCommand implements CommandExecutor, TabCompleter {
    private final LanguageManager languageManager;
    private final DraftConfig draftConfig;
    private final StatsConfig statsConfig;
    private final PreparationConfig preparationConfig;
    private final RefillConfig refillConfig;
    private final RefillManager refillManager;

    public TowersCommand(LanguageManager languageManager) {
        this.refillManager = TowersForPGM.getInstance().getRefillManager();
        this.languageManager = languageManager;
        this.draftConfig = new DraftConfig(languageManager);
        this.statsConfig = new StatsConfig(languageManager);
        this.preparationConfig = new PreparationConfig(languageManager);
        this.refillConfig = new RefillConfig(languageManager, refillManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§c/towers <draft|preparation|refill|stats|help>");
            return true;
        }
        String mainArg = args[0].toLowerCase();

        switch (mainArg) {
            case "draft":
                if (args.length == 1) {
                    sender.sendMessage("§c/towers draft <min|order|private|suggestions|timer>");
                    return true;
                }

                switch (args[1].toLowerCase()) {
                    case "min":
                        if (args.length < 3) {
                            sender.sendMessage("§c/towers draft min <size>");
                            return true;
                        }
                        try {
                            int size = Integer.parseInt(args[2]);
                            draftConfig.setMinDraftOrder(sender, size);
                        } catch (NumberFormatException e) {
                            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("draft.usage"));
                            return true;
                        }
                        break;

                    case "order":
                        if (args.length < 3) {
                            sender.sendMessage("§c/towers draft order <A[AB]+>");
                            return true;
                        }
                        String order = args[2];
                        draftConfig.setDraftOrder(sender, order);
                        break;

                    case "private":
                        if (args.length < 3 || (!args[2].equalsIgnoreCase("true") && !args[2].equalsIgnoreCase("false"))) {
                            SendMessage.sendToPlayer(sender, "§c/towers draft private <true|false>");
                            return true;
                        }
                        boolean isPrivateMatch = Boolean.parseBoolean(args[2]);
                        draftConfig.setPrivateMatch(sender, isPrivateMatch);
                        break;

                    case "suggestions":
                        draftConfig.handleDraftSuggestionsCommand(sender);
                        break;

                    case "timer":
                        draftConfig.handleDraftTimerCommand(sender);
                        break;
                    default:
                        break;
                }
                break;

            case "preparation":
                if (args.length == 1) {
                    sender.sendMessage("§c/towers preparation <toggle|add|remove|max|min|timer|haste|list>");
                    return true;
                }

                switch (args[1].toLowerCase()) {
                    case "toggle":
                        preparationConfig.togglePreparation(sender);
                        break;
                    case "add":
                        preparationConfig.handleAddCommand(sender);
                        break;
                    case "remove":
                        preparationConfig.handleRemoveCommand(sender);
                        break;
                    case "max":
                        if (args.length < 5) {
                            sender.sendMessage("§c/towers preparation max <x> <y> <z>");
                            return true;
                        }
                        try {
                            int x = Integer.parseInt(args[2]);
                            int y = Integer.parseInt(args[3]);
                            int z = Integer.parseInt(args[4]);
                            preparationConfig.handleCoordinates(sender, true, x, y, z);
                        } catch (NumberFormatException e) {
                            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("region.usage"));
                        }
                        break;
                    case "min":
                        if (args.length < 5) {
                            sender.sendMessage("§c/towers preparation min <x> <y> <z>");
                            return true;
                        }
                        try {
                            int x = Integer.parseInt(args[2]);
                            int y = Integer.parseInt(args[3]);
                            int z = Integer.parseInt(args[4]);
                            preparationConfig.handleCoordinates(sender, false, x, y, z);
                        } catch (NumberFormatException e) {
                            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("region.usage"));
                        }
                        break;
                    case "timer":
                        if (args.length < 3) {
                            sender.sendMessage("§c/towers preparation timer <mins>");
                            return true;
                        }
                        try {
                            int timer = Integer.parseInt(args[2]);
                            preparationConfig.handleTimerCommand(sender, timer);
                        } catch (NumberFormatException e) {
                            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("region.usage"));
                            return true;
                        }
                        break;
                    case "haste":
                        if (args.length < 3) {
                            sender.sendMessage("§c/towers preparation haste <mins>");
                            return true;
                        }
                        try {
                            int haste = Integer.parseInt(args[2]);
                            preparationConfig.handleHasteCommand(sender, haste);
                        } catch (NumberFormatException e) {
                            SendMessage.sendToPlayer(sender, languageManager.getPluginMessage("region.usage"));
                            return true;
                        }
                        break;
                    case "list":
                        preparationConfig.handleListCommand(sender);
                        break;
                    default:
                        break;
                }
                break;

            case "refill":
                if (args.length == 1) {
                    sender.sendMessage("§c/towers refill <add|delete|reload>");
                    return true;
                }
                Location loc = ((Player) sender).getLocation();
                String mapName = PGM.get().getMatchManager().getMatch(sender).getMap().getName();
                switch (args[1].toLowerCase()) {
                    case "add":
                        refillConfig.addRefillLocation(sender, mapName, loc);
                        return true;

                    case "delete":
                        refillConfig.removeRefillLocation(sender, mapName, loc);
                        return true;
                    
                    case "reload":
                        String worldName = ((Player) sender).getWorld().getName();
                        refillConfig.testRefill(sender, mapName, worldName);
                        return true;

                    default:
                        break;
                }

            case "stats":
                if (args.length == 1) {
                    sender.sendMessage("§c/towers stats <default|add|remove|list|addMap|removeMap>");
                    return true;
                }

                switch (args[1].toLowerCase()) {
                    case "default":
                        if (args.length < 3) {
                            sender.sendMessage("§c/towers stats default <tabla>");
                            return true;
                        }
                        String tableName = args[2];
                        statsConfig.setDefaultTable(sender, tableName);
                        break;
                    case "add":
                        if (args.length < 3) {
                            sender.sendMessage("§c/towers stats add <tabla>");
                            return true;
                        }
                        String newTableName = args[2];
                        statsConfig.addTable(sender, newTableName);
                        break;
                    case "remove":
                        if (args.length < 3) {
                            sender.sendMessage("§c/towers stats remove <tabla>");
                            return true;
                        }
                        String removeTableName = args[2];
                        statsConfig.deleteTable(sender, removeTableName);
                        break;
                    case "list":
                        statsConfig.listTables(sender);
                        break;
                    case "addmap":
                        if (args.length < 3) {
                            sender.sendMessage("§c/towers stats addMap <tabla>");
                            return true;
                        }
                        String addMapTableName = args[2];
                        statsConfig.addTableForMap(sender, addMapTableName);
                        break;
                    case "removemap":
                        statsConfig.deleteTableForMap(sender);
                        break;
                    default:
                        break;
                }
                break;

            case "help":
                if (args.length == 2) {
                    switch (args[1].toLowerCase()) {
                        case "draft":
                            HelpConfig.sendDraftHelp(sender);
                            break;
                        case "preparation":
                            HelpConfig.sendPreparationHelp(sender);
                            break;
                        case "refill":
                            HelpConfig.sendRefillHelp(sender);
                            break;
                        case "stats":
                            HelpConfig.sendStatsHelp(sender);
                            break;
                        default:
                            break;
                    }
                } else {
                    HelpConfig.sendGeneralHelp(sender);
                }
                break;

            default:
                sender.sendMessage("§c/towers help");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = Arrays.asList("draft", "preparation", "refill", "stats", "help");
            String input = args[0].toLowerCase();
            return base.stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        }

        String first = args[0].toLowerCase();

        switch (first) {
            case "draft":
                if (args.length == 2) {
                    List<String> options = Arrays.asList("min", "order", "private", "suggestions", "timer");
                    return filterPrefix(options, args[1]);
                }
                if (args.length == 3 && args[1].equalsIgnoreCase("private")) {
                    return filterPrefix(Arrays.asList("true", "false"), args[2]);
                }
                break;

            case "preparation":
                if (args.length == 2) {
                    List<String> options = Arrays.asList("toggle", "add", "remove", "max", "min", "timer", "haste", "list");
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
            case "refill":
                if (args.length == 2) {
                    List<String> options = Arrays.asList("add", "delete", "reload");
                    return filterPrefix(options, args[1]);
                }
                break;

            case "stats":
                if (args.length == 2) {
                    List<String> options = Arrays.asList("default", "add", "remove", "list", "addMap", "removeMap");
                    return filterPrefix(options, args[1]);
                }
                if (args.length == 3) {
                    String sub = args[1].toLowerCase();
                    if (Arrays.asList("default", "add", "remove", "addmap", "removemap").contains(sub)) {
                        List<String> tables = ConfigManager.getTables();
                        return filterPrefix(tables, args[2]);
                    }
                }
                break;

            case "help":
                if (args.length == 2) {
                    List<String> options = Arrays.asList("draft", "preparation", "refill", "stats");
                    return filterPrefix(options, args[1]);
                }
                break;
        }

        return null;
    }

    private List<String> filterPrefix(List<String> options, String input) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> suggestNumbers(int argPosition) {
        List<String> numbers = Arrays.asList("0", "10", "25", "50", "100");
        return numbers;
    }
}