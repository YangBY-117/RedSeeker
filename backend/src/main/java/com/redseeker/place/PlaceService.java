package com.redseeker.place;

import java.util.List;

public interface PlaceService {
  List<PlaceInfo> search(String keyword, String region, String event, Integer minHeat, Integer maxHeat, String sortBy,
      int limit);

  List<PlaceInfo> queryPlaces(PlaceQueryCondition condition);
}
