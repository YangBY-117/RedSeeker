package com.redseeker.place;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class PlaceServiceImpl implements PlaceService {
  private final List<PlaceInfo> places = buildPlaces();

  @Override
  public List<PlaceInfo> search(String keyword, String region, String event, Integer minHeat, Integer maxHeat,
      String sortBy, int limit) {
    String normalizedKeyword = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT).trim();
    String normalizedRegion = region == null ? "" : region.toLowerCase(Locale.ROOT).trim();
    String normalizedEvent = event == null ? "" : event.toLowerCase(Locale.ROOT).trim();

    List<PlaceInfo> result = new ArrayList<>();
    List<PlaceInfo> filtered = new ArrayList<>();
    for (PlaceInfo place : places) {
      if (!normalizedKeyword.isBlank() && !matchesKeyword(place, normalizedKeyword)) {
        continue;
      }
      if (!normalizedRegion.isBlank() && !place.getRegion().toLowerCase(Locale.ROOT).contains(normalizedRegion)) {
        continue;
      }
      if (!normalizedEvent.isBlank() && !place.getEvent().toLowerCase(Locale.ROOT).contains(normalizedEvent)) {
        continue;
      }
      if (minHeat != null && place.getHeatScore() < minHeat) {
        continue;
      }
      if (maxHeat != null && place.getHeatScore() > maxHeat) {
        continue;
      }
      filtered.add(place);
    }

    filtered.sort(resolveComparator(sortBy));
    for (PlaceInfo place : filtered) {
      result.add(place);
      if (result.size() >= limit) {
        break;
      }
    }
    return result;
  }

  private boolean matchesKeyword(PlaceInfo place, String keyword) {
    return place.getName().toLowerCase(Locale.ROOT).contains(keyword)
        || place.getAddress().toLowerCase(Locale.ROOT).contains(keyword)
        || place.getEvent().toLowerCase(Locale.ROOT).contains(keyword);
  }

  private List<PlaceInfo> buildPlaces() {
    List<PlaceInfo> data = new ArrayList<>();
    data.add(new PlaceInfo(1L, "中共一大会址", "上海市黄浦区兴业路76号", "上海", "建党初期", 121.4752, 31.2204, 9800, 4.8));
    data.add(new PlaceInfo(2L, "南湖红船", "浙江省嘉兴市南湖区", "浙江", "建党初期", 120.7551, 30.7566, 7200, 4.7));
    data.add(new PlaceInfo(3L, "井冈山革命博物馆", "江西省吉安市井冈山市", "江西", "土地革命", 114.1732, 26.5720, 5600, 4.6));
    data.add(new PlaceInfo(4L, "延安革命纪念馆", "陕西省延安市宝塔区", "陕西", "抗日战争", 109.4898, 36.5965, 6100, 4.5));
    data.add(new PlaceInfo(5L, "西柏坡纪念馆", "河北省石家庄市平山县", "河北", "解放战争", 113.9865, 38.3040, 4300, 4.4));
    return data;
  }

  private java.util.Comparator<PlaceInfo> resolveComparator(String sortBy) {
    if (\"rating\".equalsIgnoreCase(sortBy)) {
      return java.util.Comparator.comparingDouble(PlaceInfo::getRating).reversed();
    }
    return java.util.Comparator.comparingInt(PlaceInfo::getHeatScore).reversed();
  }
}
