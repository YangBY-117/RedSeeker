package com.redseeker.diary;

import java.util.ArrayList;
import java.util.List;

public class DiaryListResponse {
  private List<DiarySummary> diaries = new ArrayList<>();
  private int total;
  private int page;
  private int pageSize;
  private int totalPages;

  public List<DiarySummary> getDiaries() {
    return diaries;
  }

  public void setDiaries(List<DiarySummary> diaries) {
    this.diaries = diaries;
  }

  public int getTotal() {
    return total;
  }

  public void setTotal(int total) {
    this.total = total;
  }

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public int getPageSize() {
    return pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  public int getTotalPages() {
    return totalPages;
  }

  public void setTotalPages(int totalPages) {
    this.totalPages = totalPages;
  }
}
