package com.redseeker.diary;

import java.util.List;

public interface DiaryService {
  DiaryListResponse listDiaries(DiaryListQuery query);

  DiaryListResponse searchByDestination(DiaryListQuery query);

  DiarySummary searchByTitle(String title);

  DiaryListResponse fullTextSearch(String keyword, int page, int pageSize);

  DiaryDetailResponse getDiaryDetail(Long diaryId, Long userId);

  DiarySummary createDiary(DiaryCreateRequest request, Long userId);

  DiarySummary updateDiary(Long diaryId, DiaryCreateRequest request, Long userId);

  void deleteDiary(Long diaryId, Long userId);

  void deleteAllDiaries();

  DiaryRatingResponse rateDiary(Long diaryId, int rating, Long userId);

  DiaryAnimationResponse generateAnimation(Long diaryId, DiaryAnimationRequest request, Long userId);

  DiaryAnimationStatusResponse getAnimationStatus(String taskId);

  DiaryComment addComment(Long diaryId, String content, Long userId);

  List<DiaryComment> getComments(Long diaryId);
}
