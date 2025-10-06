package org.nicolie.towersforpgm.matchbot.commands.top;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.nicolie.towersforpgm.database.StatsManager;
import org.nicolie.towersforpgm.database.Top;
import org.nicolie.towersforpgm.database.TopResult;
import org.nicolie.towersforpgm.matchbot.cache.TopLRUCache;
import org.nicolie.towersforpgm.matchbot.embeds.TopEmbed;
import org.nicolie.towersforpgm.matchbot.enums.Stat;

/**
 * Listener de paginación optimizado con: - Estado en memoria por token - Keyset pagination para
 * avanzar (next) - OFFSET solo para retroceder (prev) como degradación - Evita COUNT reiterado
 * almacenando totalRecords
 *
 * <p>Formato de customId: top_prev_<token> top_next_<token>
 */
public class TopPaginationListener extends ListenerAdapter {
  public static void register() {
    net.dv8tion.jda.api.JDA jda = me.tbg.match.bot.configs.DiscordBot.getJDA();
    if (jda != null) {
      jda.addEventListener(new TopPaginationListener());
    }
  }

  public static void unregister() {
    net.dv8tion.jda.api.JDA jda = me.tbg.match.bot.configs.DiscordBot.getJDA();
    if (jda != null) {
      jda.removeEventListener(new TopPaginationListener());
    }
  }

  private static final int PAGE_SIZE = 10;
  private static final ConcurrentHashMap<String, State> STATE = new ConcurrentHashMap<>();

  private static class Anchor {
    final double value;
    final String user;

    Anchor(double value, String user) {
      this.value = value;
      this.user = user;
    }
  }

  private static class State {
    final String table;
    final Stat stat;
    final boolean perGame;
    final String dbColumn; // ya con PerGame si aplica
    final int totalRecords;
    int page; // página actual
    Anchor lastAnchor; // anchor de la página actual

    State(String table, Stat stat, boolean perGame, String dbColumn, int totalRecords) {
      this.table = table;
      this.stat = stat;
      this.perGame = perGame;
      this.dbColumn = dbColumn;
      this.totalRecords = totalRecords;
      this.page = 1;
    }
  }

  // Creado desde TopCommand cuando se envía la primera página
  public static String createStateToken(
      String table,
      Stat stat,
      boolean perGame,
      String dbColumn,
      int totalRecords,
      List<Top> firstPage) {
    String token = UUID.randomUUID().toString().substring(0, 8);
    State s = new State(table, stat, perGame, dbColumn, totalRecords);
    if (!firstPage.isEmpty()) {
      Top last = firstPage.get(firstPage.size() - 1);
      s.lastAnchor = new Anchor(last.getValue(), last.getUsername());
    }
    STATE.put(token, s);
    return token;
  }

  public static void updateAnchor(String token, List<Top> pageData) {
    State s = STATE.get(token);
    if (s == null) return;
    if (!pageData.isEmpty()) {
      Top last = pageData.get(pageData.size() - 1);
      s.lastAnchor = new Anchor(last.getValue(), last.getUsername());
    }
  }

  private static void setPage(String token, int page) {
    State s = STATE.get(token);
    if (s != null) s.page = page;
  }

  public static State getState(String token) {
    return STATE.get(token);
  }

  @Override
  public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
    String id = event.getComponentId();
    if (!(id.startsWith("top_prev_") || id.startsWith("top_next_"))) return;

    boolean isPrev = id.startsWith("top_prev_");
    // Extraer token correctamente (ambos prefijos tienen longitud 9)
    String token;
    if (isPrev) {
      token = id.substring("top_prev_".length());
    } else {
      token = id.substring("top_next_".length());
    }
    State state = getState(token);
    if (state == null) {
      event
          .reply("Estado expirado. Vuelve a usar el comando.")
          .setEphemeral(true)
          .queue();
      return;
    }

    int totalPages = Math.max(1, (int) Math.ceil((double) state.totalRecords / PAGE_SIZE));
    int currentPage = state.page;

    if (isPrev) {
      if (currentPage <= 1) {
        event.reply("No previous page available.").setEphemeral(true).queue();
        return;
      }
      int targetPage = currentPage - 1;
      // Usamos fallback OFFSET para página previa
      // Intentar caché
      TopLRUCache.CachedPage cached =
          TopLRUCache.get(state.table, state.dbColumn, targetPage, PAGE_SIZE, state.perGame);
      if (cached != null) {
        List<Top> entries = cached.data();
        setPage(token, targetPage);
        updateAnchor(token, entries);
        TopResult cachedResult = new TopResult(entries, state.totalRecords);
        EmbedBuilder embed = TopEmbed.createTopEmbed(
            state.stat, state.table, targetPage, cachedResult, state.perGame);
        event
            .editMessageEmbeds(embed.build())
            .setActionRow(
                Button.secondary("top_prev_" + token, "⬅️").withDisabled(targetPage <= 1),
                Button.secondary("top_next_" + token, "➡️").withDisabled(targetPage >= totalPages))
            .queue();
      } else {
        StatsManager.getTop(state.table, state.dbColumn, PAGE_SIZE, targetPage)
            .thenAccept(result -> {
              setPage(token, targetPage);
              updateAnchor(token, result.getData());
              EmbedBuilder embed = TopEmbed.createTopEmbed(
                  state.stat, state.table, targetPage, result, state.perGame);
              event
                  .editMessageEmbeds(embed.build())
                  .setActionRow(
                      Button.secondary("top_prev_" + token, "⬅️").withDisabled(targetPage <= 1),
                      Button.secondary("top_next_" + token, "➡️")
                          .withDisabled(targetPage >= totalPages))
                  .queue();
              double lastValuePut = result.getData().isEmpty()
                  ? 0D
                  : result.getData().get(result.getData().size() - 1).getValue();
              String lastUserPut = result.getData().isEmpty()
                  ? ""
                  : result.getData().get(result.getData().size() - 1).getUsername();
              TopLRUCache.put(
                  state.table,
                  state.dbColumn,
                  targetPage,
                  PAGE_SIZE,
                  state.perGame,
                  result.getData(),
                  result.getTotalRecords(),
                  lastValuePut,
                  lastUserPut);
            });
      }
      return;
    }

    if (currentPage >= totalPages) {
      event.reply("No next page available.").setEphemeral(true).queue();
      return;
    }

    Double lastValue = state.lastAnchor == null ? null : state.lastAnchor.value;
    String lastUser = state.lastAnchor == null ? null : state.lastAnchor.user;

    int nextPage = currentPage + 1;
    TopLRUCache.CachedPage cachedNext =
        TopLRUCache.get(state.table, state.dbColumn, nextPage, PAGE_SIZE, state.perGame);
    if (cachedNext != null) {
      List<Top> entries = cachedNext.data();
      setPage(token, nextPage);
      updateAnchor(token, entries);
      TopResult cachedNextResult = new TopResult(entries, state.totalRecords);
      EmbedBuilder embed = TopEmbed.createTopEmbed(
          state.stat, state.table, nextPage, cachedNextResult, state.perGame);
      event
          .editMessageEmbeds(embed.build())
          .setActionRow(
              Button.secondary("top_prev_" + token, "⬅️").withDisabled(nextPage <= 1),
              Button.secondary("top_next_" + token, "➡️").withDisabled(nextPage >= totalPages))
          .queue();
      return;
    }

    StatsManager.getTop(
            state.table, state.dbColumn, PAGE_SIZE, lastValue, lastUser, state.totalRecords)
        .thenAccept(result -> {
          int newPage = currentPage + 1;
          setPage(token, newPage);
          updateAnchor(token, result.getData());
          EmbedBuilder embed =
              TopEmbed.createTopEmbed(state.stat, state.table, newPage, result, state.perGame);
          event
              .editMessageEmbeds(embed.build())
              .setActionRow(
                  Button.secondary("top_prev_" + token, "⬅️").withDisabled(newPage <= 1),
                  Button.secondary("top_next_" + token, "➡️").withDisabled(newPage >= totalPages))
              .queue();
          double lastValuePut = result.getData().isEmpty()
              ? 0D
              : result.getData().get(result.getData().size() - 1).getValue();
          String lastUserPut = result.getData().isEmpty()
              ? ""
              : result.getData().get(result.getData().size() - 1).getUsername();
          TopLRUCache.put(
              state.table,
              state.dbColumn,
              newPage,
              PAGE_SIZE,
              state.perGame,
              result.getData(),
              result.getTotalRecords(),
              lastValuePut,
              lastUserPut);
        });
  }
}
