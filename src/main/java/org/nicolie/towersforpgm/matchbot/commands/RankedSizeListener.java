package org.nicolie.towersforpgm.matchbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.nicolie.towersforpgm.matchbot.embeds.RankedSize;
import org.nicolie.towersforpgm.rankeds.Queue;

/** JDA version of RankedSizeListener. */
public class RankedSizeListener extends ListenerAdapter {
  private final Queue queue;

  public RankedSizeListener(Queue queue) {
    this.queue = queue;
  }

  @Override
  public void onMessageReceived(MessageReceivedEvent event) {
    if (event.getAuthor().isBot()) return;
    String content = event.getMessage().getContentRaw();
    if (content == null) return;
    String[] parts = content.trim().split("\\s+");
    if (parts.length >= 3
        && parts[0].equalsIgnoreCase("=ranked")
        && parts[1].equalsIgnoreCase("size")) {
      if (event.getMember() != null
          && event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
        try {
          int size = Integer.parseInt(parts[2]);
          if (queue.setSize(size)) {
            EmbedBuilder embed = RankedSize.createEmbed(size);
            event.getChannel().sendMessageEmbeds(embed.build()).queue();
          }
        } catch (NumberFormatException ignored) {
        }
      }
    }
  }
}
