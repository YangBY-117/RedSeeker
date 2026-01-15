package com.redseeker.diary;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redseeker.common.ApiResponse;
import com.redseeker.common.ErrorCode;
import com.redseeker.common.ServiceException;
import com.redseeker.user.UserService;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/diary")
public class DiaryController {
  private final DiaryService diaryService;
  private final UserService userService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public DiaryController(DiaryService diaryService, UserService userService) {
    this.diaryService = diaryService;
    this.userService = userService;
  }

  @GetMapping("/list")
  public ApiResponse<DiaryListResponse> listDiaries(
      @RequestParam(value = "sortBy", required = false) String sortBy,
      @RequestParam(value = "destination", required = false) String destination,
      @RequestParam(value = "userId", required = false) Long userId,
      @RequestParam(value = "page", defaultValue = "1") int page,
      @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
    DiaryListQuery query = new DiaryListQuery();
    query.setSortBy(sortBy);
    query.setDestination(destination);
    query.setUserId(userId);
    query.setPage(page);
    query.setPageSize(pageSize);
    return ApiResponse.ok(diaryService.listDiaries(query));
  }

  @GetMapping("/search-by-destination")
  public ApiResponse<DiaryListResponse> searchByDestination(
      @RequestParam("destination") String destination,
      @RequestParam(value = "sortBy", required = false) String sortBy,
      @RequestParam(value = "page", defaultValue = "1") int page,
      @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
    DiaryListQuery query = new DiaryListQuery();
    query.setSortBy(sortBy);
    query.setDestination(destination);
    query.setPage(page);
    query.setPageSize(pageSize);
    return ApiResponse.ok(diaryService.searchByDestination(query));
  }

  @GetMapping("/search-by-name")
  public ApiResponse<DiarySummary> searchByName(@RequestParam("title") String title) {
    return ApiResponse.ok(diaryService.searchByTitle(title));
  }

  @GetMapping("/fulltext-search")
  public ApiResponse<DiaryListResponse> fullTextSearch(
      @RequestParam("keyword") String keyword,
      @RequestParam(value = "page", defaultValue = "1") int page,
      @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
    return ApiResponse.ok(diaryService.fullTextSearch(keyword, page, pageSize));
  }

  @GetMapping("/{id}")
  public ApiResponse<DiaryDetailResponse> getDetail(
      @PathVariable("id") Long id,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    Long userId = resolveUserId(authHeader);
    return ApiResponse.ok(diaryService.getDiaryDetail(id, userId));
  }

  @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<DiarySummary> createDiary(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestParam("title") String title,
      @RequestParam("content") String content,
      @RequestParam(value = "destination", required = false) String destination,
      @RequestParam(value = "travel_date", required = false) String travelDate,
      @RequestParam(value = "attraction_ids", required = false) String attractionIds,
      @RequestParam(value = "images", required = false) List<MultipartFile> images,
      @RequestParam(value = "videos", required = false) List<MultipartFile> videos) {
    Long userId = requireUserId(authHeader);
    DiaryCreateRequest request =
        buildCreateRequest(title, content, destination, travelDate, attractionIds, images, videos);
    return ApiResponse.ok(diaryService.createDiary(request, userId));
  }

  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<DiarySummary> updateDiary(
      @PathVariable("id") Long id,
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestParam("title") String title,
      @RequestParam("content") String content,
      @RequestParam(value = "destination", required = false) String destination,
      @RequestParam(value = "travel_date", required = false) String travelDate,
      @RequestParam(value = "attraction_ids", required = false) String attractionIds,
      @RequestParam(value = "images", required = false) List<MultipartFile> images,
      @RequestParam(value = "videos", required = false) List<MultipartFile> videos) {
    Long userId = requireUserId(authHeader);
    DiaryCreateRequest request =
        buildCreateRequest(title, content, destination, travelDate, attractionIds, images, videos);
    return ApiResponse.ok(diaryService.updateDiary(id, request, userId));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Boolean> deleteDiary(
      @PathVariable("id") Long id,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    Long userId = requireUserId(authHeader);
    diaryService.deleteDiary(id, userId);
    return ApiResponse.ok(true);
  }

  @PostMapping("/{id}/rate")
  public ApiResponse<DiaryRatingResponse> rateDiary(
      @PathVariable("id") Long id,
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestBody DiaryRateRequest request) {
    Long userId = requireUserId(authHeader);
    return ApiResponse.ok(diaryService.rateDiary(id, request.getRating(), userId));
  }

  @PostMapping("/{id}/generate-animation")
  public ApiResponse<DiaryAnimationResponse> generateAnimation(
      @PathVariable("id") Long id,
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestBody DiaryAnimationRequest request) {
    Long userId = requireUserId(authHeader);
    return ApiResponse.ok(diaryService.generateAnimation(id, request, userId));
  }

  @GetMapping("/animation-status/{taskId}")
  public ApiResponse<DiaryAnimationStatusResponse> animationStatus(@PathVariable("taskId") String taskId) {
    return ApiResponse.ok(diaryService.getAnimationStatus(taskId));
  }

  private DiaryCreateRequest buildCreateRequest(
      String title,
      String content,
      String destination,
      String travelDate,
      String attractionIds,
      List<MultipartFile> images,
      List<MultipartFile> videos) {
    DiaryCreateRequest request = new DiaryCreateRequest();
    request.setTitle(title);
    request.setContent(content);
    request.setDestination(destination);
    request.setTravelDate(travelDate);
    request.setImages(images != null ? images : Collections.emptyList());
    request.setVideos(videos != null ? videos : Collections.emptyList());
    request.setAttractionIds(parseAttractionIds(attractionIds));
    return request;
  }

  private List<Long> parseAttractionIds(String raw) {
    if (raw == null || raw.isBlank()) {
      return Collections.emptyList();
    }
    try {
      return objectMapper.readValue(raw, new TypeReference<List<Long>>() {});
    } catch (Exception ex) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "invalid attraction_ids");
    }
  }

  private Long requireUserId(String authHeader) {
    Long userId = resolveUserId(authHeader);
    if (userId == null) {
      throw new ServiceException(ErrorCode.UNAUTHORIZED, "Authorization token is required");
    }
    return userId;
  }

  private Long resolveUserId(String authHeader) {
    if (authHeader == null || authHeader.isBlank()) {
      return null;
    }
    String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
    return userService.resolveUserId(token);
  }

  public static class DiaryRateRequest {
    @NotNull
    private Integer rating;

    public Integer getRating() {
      return rating;
    }

    public void setRating(Integer rating) {
      this.rating = rating;
    }
  }
}
