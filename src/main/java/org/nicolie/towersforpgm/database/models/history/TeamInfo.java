package org.nicolie.towersforpgm.database.models.history;

public class TeamInfo {
  private final String teamName;
  private final String colorHex;
  private final boolean isWinner;
  private final Integer score;

  public TeamInfo(String teamName, String colorHex, boolean isWinner, Integer score) {
    this.teamName = teamName;
    this.colorHex = colorHex;
    this.isWinner = isWinner;
    this.score = score;
  }

  public String getTeamName() {
    return teamName;
  }

  public String getColorHex() {
    return colorHex;
  }

  public boolean isWinner() {
    return isWinner;
  }

  public Integer getScore() {
    return score;
  }
}
