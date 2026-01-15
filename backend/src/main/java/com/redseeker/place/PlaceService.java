package com.redseeker.place;

import java.util.List;
import java.util.Map;

public interface PlaceService {
    Map<String, Object> searchNearbyPlaces(PlaceAroundRequest request);
    List<PlaceCandidate> sortByRealDistance(PlaceDistanceSortRequest request);
}
