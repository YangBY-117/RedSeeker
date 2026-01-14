package com.redseeker.diary;

public interface DiaryService {
  DiaryListResponse listDiaries(String sortBy, String destination, Long userId, int page, int pageSize);

  DiaryListResponse searchByDestination(String destination, String sortBy, int page, int pageSize);

  DiaryDetail searchByName(String title);

  DiaryListResponse fulltextSearch(String keyword, int page, int pageSize);

  DiaryDetail getDiary(Long id, Long userId);

  DiaryCreateResponse createDiary(DiaryCreateRequest request);

  DiaryCreateResponse updateDiary(Long id, DiaryCreateRequest request);

  void deleteDiary(Long id, Long userId);

  DiaryRateResponse rateDiary(Long diaryId, DiaryRatingRequest request);
}
