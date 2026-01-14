package com.redseeker.diary;

import java.util.List;

public class DiaryListResponse {
  private List<DiarySummary> diaries;
  private long total;
  private int page;
  private int pageSize;
  private int totalPages;

  public DiaryListResponse() {
  }

  public DiaryListResponse(List<DiarySummary> diaries, long total, int page, int pageSize,
      int totalPages) {
    this.diaries = diaries;
    this.total = total;
    this.page = page;
    this.pageSize = pageSize;
    this.totalPages = totalPages;
  }

  public List<DiarySummary> getDiaries() {
    return diaries;
  }

  public void setDiaries(List<DiarySummary> diaries) {
    this.diaries = diaries;
  }

  public long getTotal() {
    return total;
  }

  public void setTotal(long total) {
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
