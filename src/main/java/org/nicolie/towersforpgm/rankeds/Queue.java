package org.nicolie.towersforpgm.rankeds;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.draft.Draft;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;

import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.Sounds;

public class Queue {
    private final TowersForPGM plugin = TowersForPGM.getInstance();
    private final Draft draft;
    private static final List<UUID> queuePlayers = new java.util.ArrayList<>();
    private static boolean countdownActive = false;
    public static final String RANKED_PREFIX = "§8[§6Ranked§8]§r ";
    private final LanguageManager languageManager;

    public Queue(Draft draft, LanguageManager languageManager) {
        this.draft = draft;
        this.languageManager = languageManager;
    }

    public void setSize(CommandSender sender, int size){
        if (size == ConfigManager.getRankedSize()){return;}
        if (size < 4 || size % 2 != 0) {
            MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer((Player) sender);
            matchPlayer.sendWarning(Component.text(RANKED_PREFIX + languageManager.getPluginMessage("ranked.sizeInvalid")));
            RankedPlayers.clearCaptainHistory();
            return;
        }
        Match match = PGM.get().getMatchManager().getMatch(sender);
        ConfigManager.setRankedSize(size);
        String message = RANKED_PREFIX + languageManager.getPluginMessage("ranked.sizeSet")
                .replace("{size}", String.valueOf(size));
        match.sendMessage(Component.text(message));
        if (queuePlayers.size() >= ConfigManager.getRankedSize()) {
            startRanked(match);
        }
    }

    public void addPlayer(MatchPlayer player){
        UUID playerUUID = player.getId();
        if(player.isParticipating() || player.getMatch().isRunning()){
            player.sendWarning(Component.text(RANKED_PREFIX + languageManager.getPluginMessage("ranked.matchInProgress")));
            return;
        }
        if(queuePlayers.contains(playerUUID)) {
            player.sendWarning(Component.text(RANKED_PREFIX + languageManager.getPluginMessage("ranked.alreadyInQueue")));
            return;
        }
        Match match = player.getMatch();
        String map = match.getMap().getName();
        if(!ConfigManager.getRankedMaps().contains(map)) {
            player.sendWarning(Component.text(RANKED_PREFIX + languageManager.getPluginMessage("ranked.notRankedMap")
                    .replace("{map}", map)));
            return;
        }
        queuePlayers.add(playerUUID);
        Component message = Component.text(RANKED_PREFIX + languageManager.getPluginMessage("ranked.joinedQueue")
                .replace("{player}", player.getPrefixedName())
                .replace("{size}", String.valueOf(queuePlayers.size()))
                .replace("{max}", String.valueOf(ConfigManager.getRankedSize())));
        match.sendMessage(message);
        if (queuePlayers.size() >= ConfigManager.getRankedSize()) {
            startRanked(match);
        }
    }

    public void removePlayer(MatchPlayer player){
        UUID playerUUID = player.getId();
        if(!queuePlayers.contains(playerUUID)) {
            player.sendWarning(Component.text(RANKED_PREFIX + languageManager.getPluginMessage("ranked.notInQueue")));
            return;
        }
        queuePlayers.remove(playerUUID);
        Component message = Component.text(RANKED_PREFIX + languageManager.getPluginMessage("ranked.leftQueue")
                .replace("{player}", player.getPrefixedName())
                .replace("{size}", String.valueOf(queuePlayers.size()))
                .replace("{max}", String.valueOf(ConfigManager.getRankedSize())));
        player.getMatch().sendMessage(message);
    }

    public static void removePlayer(MatchPlayer player, LanguageManager languageManager) {
        UUID playerUUID = player.getId();
        if(!queuePlayers.contains(playerUUID)) {
            player.sendWarning(Component.text(RANKED_PREFIX + languageManager.getPluginMessage("ranked.notInQueue")));
            return;
        }
        queuePlayers.remove(playerUUID);
        Component message = Component.text(RANKED_PREFIX + languageManager.getPluginMessage("ranked.leftQueue")
                .replace("{player}", player.getPrefixedName())
                .replace("{size}", String.valueOf(queuePlayers.size()))
                .replace("{max}", String.valueOf(ConfigManager.getRankedSize())));
        player.getMatch().sendMessage(message);
    }

    public void startRanked(Match match){
        if (match.getPhase() == MatchPhase.RUNNING || match.getPhase() == MatchPhase.FINISHED || countdownActive) {
            return;
        }
        countdownActive = true;
        final int[] countdown = { 5 };
        new BukkitRunnable() {
            @Override
            public void run() {
                if(countdown[0] <= 0){
                    // Obtener a los primeros jugadores y borrarlos de la queue
                    List<String> rankedPlayers = queuePlayers
                            .subList(0, ConfigManager.getRankedSize()).stream()
                            .map(uuid -> PGM.get().getMatchManager().getPlayer(uuid).getNameLegacy())
                            .collect(Collectors.toList());
                    // Borrar los jugadores de la cola
                    queuePlayers.subList(0, ConfigManager.getRankedSize()).clear();
                    // Obtener el elo
                    String table = ConfigManager.getRankedDefaultTable();
                    StatsManager.getEloForUsernames(table, rankedPlayers, eloList -> {
                        List<Map.Entry<MatchPlayer, Integer>> playersWithElo = eloList.stream()
                            .map(e -> {
                                UUID uuid = getUUIDFromUsername(e.getUsername());
                                MatchPlayer player = PGM.get().getMatchManager().getPlayer(uuid);
                                return new AbstractMap.SimpleEntry<>(player, e.getCurrentElo());
                            })
                            .filter(entry -> entry.getKey() != null) // Validar que el MatchPlayer no sea null
                            .collect(Collectors.toList());

                        // Seleccionar capitanes
                        RankedPlayers pair = RankedPlayers.selectCaptains(playersWithElo);

                        // Aquí podrías hacer algo con los capitanes seleccionados
                        UUID captain1 = pair.getCaptain1();
                        UUID captain2 = pair.getCaptain2();
                        List<MatchPlayer> remaining = pair.getRemainingPlayers();

                        // Iniciar el draft
                        ConfigManager.addTempTable(table);
                        draft.setCustomOrderPattern(ConfigManager.getRankedOrder(), 0);
                        draft.startDraft(captain1, captain2, remaining, match);
                    });
                    // Cancelar el countdown
                        this.cancel();
                        countdownActive = false;
                        return;
                }
                if (queuePlayers.size() < ConfigManager.getRankedSize()) {
                    this.cancel();
                    countdownActive = false;
                    match.sendWarning(Component.text(RANKED_PREFIX + languageManager.getPluginMessage("ranked.cancelled")));
                    return;
                }
                match.sendMessage(Component.text(RANKED_PREFIX + languageManager.getPluginMessage("ranked.countdown")
                        .replace("{time}", String.valueOf(countdown[0]))));
                match.playSound(Sounds.INVENTORY_CLICK);
                countdown[0]--;
            }
        }.runTaskTimer(plugin, 0, 20L);
    }

    public List<String> getQueueList() {
        List<String> players = new ArrayList<>();
        for (UUID uuid : queuePlayers) {
            MatchPlayer player = PGM.get().getMatchManager().getPlayer(uuid);
            if (player != null) {
                String playerName = player.getPrefixedName();
                players.add(playerName);
            }
        }
        return players;
    }

    public int getQueueSize() {
        return queuePlayers.size();
    }

    public List<UUID> getQueuePlayers() {
        return new ArrayList<>(queuePlayers);
    }

    private UUID getUUIDFromUsername(String username) {
        OfflinePlayer offline = Bukkit.getPlayerExact(username);
        return offline != null ? offline.getUniqueId() : null;
    }

}
