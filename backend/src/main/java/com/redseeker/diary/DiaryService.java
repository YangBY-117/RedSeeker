package com.redseeker.diary;

import java.util.List;

public interface DiaryService {
  DiaryEntry create(DiaryRequest request);

  DiaryEntry update(Long id, DiaryRequest request);

  DiaryEntry get(Long id);

  List<DiaryEntry> list();

  DiaryEntry share(Long id);
}
