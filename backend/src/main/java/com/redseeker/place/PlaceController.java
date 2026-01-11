package com.redseeker.place;

import com.redseeker.common.ApiResponse;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/place")
@Validated
public class PlaceController {
  private final PlaceService placeService;

  public PlaceController(PlaceService placeService) {
    this.placeService = placeService;
  }

  @GetMapping("/search")
  public ApiResponse<PlaceSearchResponse> search(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String region,
      @RequestParam(required = false) String event,
      @RequestParam(required = false) @Min(0) Integer minHeat,
      @RequestParam(required = false) @Min(0) Integer maxHeat,
      @RequestParam(required = false, defaultValue = "heat") String sortBy,
      @RequestParam(defaultValue = "10") @Positive int limit) {
    List<PlaceInfo> places = placeService.search(keyword, region, event, minHeat, maxHeat, sortBy, limit);
    return ApiResponse.ok(new PlaceSearchResponse(places));
  }
}
