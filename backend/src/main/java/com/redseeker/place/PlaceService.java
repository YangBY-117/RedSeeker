package com.redseeker.place;

import java.util.List;

public interface PlaceService {
  List<Place> queryPlaces(PlaceQueryCondition condition);
}
