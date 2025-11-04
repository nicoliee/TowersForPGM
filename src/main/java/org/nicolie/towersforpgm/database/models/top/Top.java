package org.nicolie.towersforpgm.database.models.top;

public class Top {
  private final String username;
  private final double value;

  public Top(String username, double value) {
    this.username = username;
    this.value = value;
  }

  public String getUsername() {
    return username;
  }

  public double getValue() {
    return value;
  }

  public int getValueAsInt() {
    return (int) value;
  }

  public int getValueAsPercent() {
    return (int) (value * 100);
  }

  @Override
  public String toString() {
    return "Top{" + "username='" + username + '\'' + ", value=" + value + '}';
  }
}
