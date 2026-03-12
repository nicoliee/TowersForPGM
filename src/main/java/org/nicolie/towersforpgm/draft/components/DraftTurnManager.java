package org.nicolie.towersforpgm.draft.components;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.nicolie.towersforpgm.draft.core.AvailablePlayers;
import org.nicolie.towersforpgm.draft.core.Captains;
import org.nicolie.towersforpgm.draft.core.Teams;
import org.nicolie.towersforpgm.utils.MatchManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

public class DraftTurnManager {
  private final AvailablePlayers availablePlayers;
  private final DraftState state;
  private final Captains captains;
  private final Teams teams;

  public DraftTurnManager(
      AvailablePlayers availablePlayers, DraftState state, Captains captains, Teams teams) {
    this.availablePlayers = availablePlayers;
    this.state = state;
    this.captains = captains;
    this.teams = teams;
  }

  public void setCustomOrderPattern(String pattern, int minPlayers) {
    if (pattern != null && !pattern.isEmpty()) {
      state.setUsingCustomPattern(true);
      state.setCurrentPatternIndex(0);
      state.setCustomOrderPattern(pattern.toUpperCase());
      state.setCustomOrderMinPlayers(minPlayers);
    }
  }

  public void resetPattern() {
    state.resetPattern();
  }

  public void resetIndex() {
    state.setCurrentPatternIndex(0);
  }

  public void resetTurnOrder() {
    state.setCurrentPatternIndex(0);
    // No borrar el custom pattern, solo resetear el índice
    // La flag de usingCustomPattern se mantiene si había un patrón activo
  }

  public void updateTurnOrder() {
    if (state.isUsingCustomPattern()) {
      state.setCurrentPatternIndex(state.getCurrentPatternIndex() + 1);

      // Si hemos llegado al final del patrón, alternamos el turno
      if (state.getCurrentPatternIndex() >= state.getCustomOrderPattern().length()) {
        state.setUsingCustomPattern(false);
        captains.toggleTurn();
        captains.setPlayerSuggestions(false);
        availablePlayers.clearSuggestions();
      } else {
        // Obtener el siguiente capitán según el patrón
        char nextCaptain = state.getCustomOrderPattern().charAt(state.getCurrentPatternIndex());
        boolean shouldBeCaptain1Turn;

        if (nextCaptain == 'A') {
          // Si es 'A', debe ser el turno del capitán que pickeo primero
          shouldBeCaptain1Turn = state.isFirstCaptainTurn();
        } else {
          // Si es 'B', debe ser el turno del otro capitán
          shouldBeCaptain1Turn = !state.isFirstCaptainTurn();
        }
        // Si un capitán debe pickear seguido enviar un mensaje
        if ((shouldBeCaptain1Turn == captains.isCaptain1Turn())
            || (!shouldBeCaptain1Turn == !captains.isCaptain1Turn())) {
          int currentTeamNumber = captains.isCaptain1Turn() ? 1 : 2;
          MatchPlayer currentCaptain = PGM.get()
              .getMatchManager()
              .getPlayer(
                  captains.isCaptain1Turn() ? captains.getCaptain1() : captains.getCaptain2());
          Component captainName = currentCaptain != null
              ? currentCaptain.getName()
              : teams.getTeam(currentTeamNumber).getName();

          MatchManager.getMatch()
              .sendMessage(Component.translatable(
                      "draft.turn",
                      captainName.color(teams.getTeam(currentTeamNumber).getTextColor()))
                  .color(NamedTextColor.GRAY));
        }
        captains.setCaptain1Turn(shouldBeCaptain1Turn);
        captains.setPlayerSuggestions(false);
        availablePlayers.clearSuggestions();
      }
    } else {
      captains.toggleTurn();
      captains.setPlayerSuggestions(false);
      availablePlayers.clearSuggestions();
    }
  }
}
