package org.nicolie.towersforpgm.commands;

import java.util.ArrayList;
import java.util.List;
import me.tbg.match.bot.configs.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nicolie.towersforpgm.TowersForPGM;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.matchbot.MatchBotConfig;
import org.nicolie.towersforpgm.matchbot.embeds.RankedNotify;
import org.nicolie.towersforpgm.utils.ConfigManager;
import org.nicolie.towersforpgm.utils.LanguageManager;
import org.nicolie.towersforpgm.utils.SendMessage;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class TagCommand implements CommandExecutor {
  private static long lastExecution = 0;
  private static final long COOLDOWN_MILLIS = 30 * 60 * 1000;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!TowersForPGM.getInstance().isMatchBotEnabled()) {
      return true;
    }
    if (!(sender instanceof Player)) {
      SendMessage.sendToConsole(LanguageManager.langMessage("errors.noPlayer"));
      return true;
    }
    Match match = PGM.get().getMatchManager().getMatch(sender);
    String map = match.getMap().getName();
    MatchPlayer player = match.getPlayer((Player) sender);
    if (!ConfigManager.getRankedMaps().contains(map)) {
      player.sendWarning(Component.text(LanguageManager.langMessage("ranked.prefix")
          + LanguageManager.langMessage("ranked.notRankedMap")));
      return true;
    }
    if (player.isParticipating()) {
      player.sendWarning(
          Component.text(LanguageManager.langMessage("ranked.matchbot.tagNotAvailable")));
      return true;
    }
    long now = System.currentTimeMillis();
    if (now - lastExecution < COOLDOWN_MILLIS) {
      long remaining = (COOLDOWN_MILLIS - (now - lastExecution)) / 1000;
      long min = remaining / 60;
      long sec = remaining % 60;
      String time = String.format("%d:%02d", min, sec);
      player.sendWarning(Component.text(
          LanguageManager.langMessage("ranked.matchbot.tagCooldown").replace("{time}", time)));
      return true;
    }
    lastExecution = now;
    sendEmbed(sender, match, player);
    return true;
  }

  private void sendEmbed(CommandSender sender, Match match, MatchPlayer player) {
    List<String> usernames = new ArrayList<>();
    for (MatchPlayer matchPlayer : match.getPlayers()) {
      usernames.add(matchPlayer.getNameLegacy());
    }
    StatsManager.getEloForUsernames(ConfigManager.getRankedDefaultTable(), usernames)
        .thenAccept(eloChanges -> {
          EmbedBuilder embed = RankedNotify.create(sender, match, eloChanges);
          DiscordBot.setEmbedThumbnail(match.getMap(), embed);
          DiscordBot.sendMatchEmbed(
              embed, match, MatchBotConfig.getDiscordChannel(), MatchBotConfig.getRankedRoleId());
          org.bukkit.Bukkit.getScheduler()
              .runTask(
                  TowersForPGM.getInstance(),
                  () ->
                      match.sendMessage(Component.text(LanguageManager.langMessage("ranked.prefix")
                          + LanguageManager.langMessage("ranked.matchbot.tagSent")
                              .replace("{name}", player.getPrefixedName()))));
        })
        .exceptionally(throwable -> {
          return null;
        });
  }
}
