package com.redseeker.diary;

import com.redseeker.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/diary")
public class DiaryController {
  private final DiaryService diaryService;

  public DiaryController(DiaryService diaryService) {
    this.diaryService = diaryService;
  }

  @GetMapping("/list")
  public ApiResponse<DiaryListResponse> list(
      @RequestParam(value = "sortBy", required = false) String sortBy,
      @RequestParam(value = "destination", required = false) String destination,
      @RequestParam(value = "userId", required = false) Long userId,
      @RequestParam(value = "page", defaultValue = "1") int page,
      @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
    return ApiResponse.ok(diaryService.listDiaries(sortBy, destination, userId, page, pageSize));
  }

  @GetMapping("/search-by-destination")
  public ApiResponse<DiaryListResponse> searchByDestination(
      @RequestParam("destination") String destination,
      @RequestParam(value = "sortBy", required = false) String sortBy,
      @RequestParam(value = "page", defaultValue = "1") int page,
      @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
    return ApiResponse.ok(diaryService.searchByDestination(destination, sortBy, page, pageSize));
  }

  @GetMapping("/search-by-name")
  public ApiResponse<DiaryDetail> searchByName(@RequestParam("title") String title) {
    return ApiResponse.ok(diaryService.searchByName(title));
  }

  @GetMapping("/fulltext-search")
  public ApiResponse<DiaryListResponse> fulltextSearch(
      @RequestParam("keyword") String keyword,
      @RequestParam(value = "page", defaultValue = "1") int page,
      @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
    return ApiResponse.ok(diaryService.fulltextSearch(keyword, page, pageSize));
  }

  @GetMapping("/{id}")
  public ApiResponse<DiaryDetail> detail(
      @PathVariable("id") Long id,
      @RequestParam(value = "userId", required = false) Long userId) {
    return ApiResponse.ok(diaryService.getDiary(id, userId));
  }

  @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<DiaryCreateResponse> create(
      @RequestParam("userId") Long userId,
      @RequestParam("title") String title,
      @RequestParam("content") String content,
      @RequestParam("destination") String destination,
      @RequestParam("travel_date") String travelDate,
      @RequestParam(value = "attraction_ids", required = false) List<Long> attractionIds,
      @RequestPart(value = "images", required = false) List<MultipartFile> images,
      @RequestPart(value = "videos", required = false) List<MultipartFile> videos) {
    DiaryCreateRequest request = buildCreateRequest(
        userId,
        title,
        content,
        destination,
        travelDate,
        attractionIds,
        images,
        videos);
    return ApiResponse.ok(diaryService.createDiary(request));
  }

  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<DiaryCreateResponse> update(
      @PathVariable("id") Long id,
      @RequestParam("userId") Long userId,
      @RequestParam("title") String title,
      @RequestParam("content") String content,
      @RequestParam("destination") String destination,
      @RequestParam("travel_date") String travelDate,
      @RequestParam(value = "attraction_ids", required = false) List<Long> attractionIds,
      @RequestPart(value = "images", required = false) List<MultipartFile> images,
      @RequestPart(value = "videos", required = false) List<MultipartFile> videos) {
    DiaryCreateRequest request = buildCreateRequest(
        userId,
        title,
        content,
        destination,
        travelDate,
        attractionIds,
        images,
        videos);
    return ApiResponse.ok(diaryService.updateDiary(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @PathVariable("id") Long id,
      @RequestParam("userId") Long userId) {
    diaryService.deleteDiary(id, userId);
    return ApiResponse.ok(null);
  }

  @PostMapping("/{id}/rate")
  public ApiResponse<DiaryRateResponse> rate(
      @PathVariable("id") Long id,
      @Valid @RequestBody DiaryRatingRequest request) {
    return ApiResponse.ok(diaryService.rateDiary(id, request));
  }

  private DiaryCreateRequest buildCreateRequest(
      Long userId,
      String title,
      String content,
      String destination,
      String travelDate,
      List<Long> attractionIds,
      List<MultipartFile> images,
      List<MultipartFile> videos) {
    DiaryCreateRequest request = new DiaryCreateRequest();
    request.setUserId(userId);
    request.setTitle(title);
    request.setContent(content);
    request.setDestination(destination);
    request.setTravelDate(travelDate);
    request.setAttractionIds(attractionIds);
    request.setMediaInputs(buildMediaInputs(images, videos));
    return request;
  }

  private List<DiaryMediaInput> buildMediaInputs(List<MultipartFile> images,
      List<MultipartFile> videos) {
    List<DiaryMediaInput> media = new ArrayList<>();
    int order = 0;
    if (images != null) {
      for (MultipartFile file : images) {
        if (file == null || file.isEmpty()) {
          continue;
        }
        media.add(new DiaryMediaInput(
            "image",
            file.getOriginalFilename(),
            file.getSize(),
            null,
            order++));
      }
    }
    if (videos != null) {
      for (MultipartFile file : videos) {
        if (file == null || file.isEmpty()) {
          continue;
        }
        media.add(new DiaryMediaInput(
            "video",
            file.getOriginalFilename(),
            file.getSize(),
            null,
            order++));
      }
    }
    return media;
  }
}
