package com.redseeker.place;

import java.util.List;

public interface PlaceService {
  List<PlaceCandidate> sortByRealDistance(PlaceDistanceSortRequest request);
}
