package com.redseeker.place;

import com.redseeker.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/place")
public class PlaceController {
  private final PlaceService placeService;

  public PlaceController(PlaceService placeService) {
    this.placeService = placeService;
  }

  @PostMapping("/distance-sort")
  public ApiResponse<List<PlaceCandidate>> distanceSort(
      @Valid @RequestBody PlaceDistanceSortRequest request) {
    return ApiResponse.ok(placeService.sortByRealDistance(request));
  }
}
