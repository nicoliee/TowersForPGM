package org.nicolie.towersforpgm.database.models.top;

import java.util.List;

public class TopResult {
  private final List<Top> data;
  private final int totalRecords;

  public TopResult(List<Top> data, int totalRecords) {
    this.data = data;
    this.totalRecords = totalRecords;
  }

  public List<Top> getData() {
    return data;
  }

  public int getTotalRecords() {
    return totalRecords;
  }

  public int getTotalPages(int pageSize) {
    return Math.max(1, (int) Math.ceil((double) totalRecords / pageSize));
  }

  public boolean hasNextPage(int currentPage, int pageSize) {
    return currentPage < getTotalPages(pageSize);
  }

  public boolean hasPreviousPage(int currentPage) {
    return currentPage > 1;
  }
}
