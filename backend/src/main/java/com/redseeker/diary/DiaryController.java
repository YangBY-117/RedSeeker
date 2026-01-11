package com.redseeker.diary;

import com.redseeker.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diary")
public class DiaryController {
  private final DiaryService diaryService;

  public DiaryController(DiaryService diaryService) {
    this.diaryService = diaryService;
  }

  @PostMapping
  public ApiResponse<DiaryEntry> create(@Valid @RequestBody DiaryRequest request) {
    return ApiResponse.ok(diaryService.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<DiaryEntry> update(@PathVariable Long id, @RequestBody DiaryRequest request) {
    return ApiResponse.ok(diaryService.update(id, request));
  }

  @GetMapping("/{id}")
  public ApiResponse<DiaryEntry> get(@PathVariable Long id) {
    return ApiResponse.ok(diaryService.get(id));
  }

  @GetMapping
  public ApiResponse<List<DiaryEntry>> list() {
    return ApiResponse.ok(diaryService.list());
  }

  @PostMapping("/{id}/share")
  public ApiResponse<DiaryEntry> share(@PathVariable Long id) {
    return ApiResponse.ok(diaryService.share(id));
  }
}
