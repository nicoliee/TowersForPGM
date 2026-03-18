package org.nicolie.towersforpgm.draft.pick;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.nicolie.towersforpgm.draft.state.DraftState;
import org.nicolie.towersforpgm.draft.team.AvailablePlayers;
import org.nicolie.towersforpgm.draft.team.Captains;
import org.nicolie.towersforpgm.draft.team.Teams;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class DraftTurnManager {
  private final Match match;
  private final AvailablePlayers availablePlayers;
  private final DraftState state;
  private final Captains captains;
  private final Teams teams;

  public DraftTurnManager(
      Match match,
      AvailablePlayers availablePlayers,
      DraftState state,
      Captains captains,
      Teams teams) {
    this.match = match;
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

  public Component getOrderStyled() {
    if (!state.isUsingCustomPattern()) {
      return null;
    }
    String pattern = state.getCustomOrderPattern();
    int currentIndex = state.getCurrentPatternIndex();
    Component result = Component.empty();
    for (int i = 0; i < pattern.length(); i++) {
      Component letter = Component.text(String.valueOf(pattern.charAt(i)));
      letter = i == currentIndex
          ? letter.color(NamedTextColor.WHITE)
          : letter.color(NamedTextColor.GRAY);
      result = result.append(letter);
    }
    return result;
  }

  public void updateTurnOrder() {
    if (state.isUsingCustomPattern()) {
      updateCustomTurnOrder();
    } else {
      toggleTurnAndClear();
    }
  }

  private void updateCustomTurnOrder() {
    state.setCurrentPatternIndex(state.getCurrentPatternIndex() + 1);
    if (state.getCurrentPatternIndex() >= state.getCustomOrderPattern().length()) {
      endPatternTurn();
    } else {
      applyNextPatternTurn();
    }
  }

  private void endPatternTurn() {
    state.setUsingCustomPattern(false);
    toggleTurnAndClear();
  }

  private void applyNextPatternTurn() {
    boolean shouldBeCaptain1Turn = getNextCaptainTurn();
    if ((shouldBeCaptain1Turn == captains.isCaptain1Turn())
        || (!shouldBeCaptain1Turn == !captains.isCaptain1Turn())) {
      // Si le toca al mismo capitán que ya tenía el turno enviar el mensaje
      sendDraftTurnMessage();
    }
    captains.setCaptain1Turn(shouldBeCaptain1Turn);
    clearSuggestions();
  }

  private boolean getNextCaptainTurn() {
    char nextCaptain = state.getCustomOrderPattern().charAt(state.getCurrentPatternIndex());
    if (nextCaptain == 'A') {
      return state.isFirstCaptainTurn();
    }
    return !state.isFirstCaptainTurn();
  }

  private void sendDraftTurnMessage() {
    int currentTeamNumber = captains.isCaptain1Turn() ? 1 : 2;
    MatchPlayer currentCaptain = PGM.get()
        .getMatchManager()
        .getPlayer(captains.isCaptain1Turn() ? captains.getCaptain1() : captains.getCaptain2());
    Component captainName = currentCaptain != null
        ? currentCaptain.getName()
        : teams.getTeam(currentTeamNumber).getName();

    match.sendMessage(Component.translatable(
            "draft.turn", captainName.color(teams.getTeam(currentTeamNumber).getTextColor()))
        .color(NamedTextColor.GRAY));
  }

  private void toggleTurnAndClear() {
    captains.toggleTurn();
    clearSuggestions();
  }

  private void clearSuggestions() {
    captains.setPlayerSuggestions(false);
    availablePlayers.clearSuggestions();
  }
}
