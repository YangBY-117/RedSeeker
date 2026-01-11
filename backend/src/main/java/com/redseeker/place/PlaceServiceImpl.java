package com.redseeker.place;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class PlaceServiceImpl implements PlaceService {
  private static final String DATABASE_PATH = "database/red_tourism.db";

  @Override
  public List<PlaceInfo> search(String keyword, String region, String event, Integer minHeat, Integer maxHeat,
      String sortBy, int limit) {
    String normalizedKeyword = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT).trim();
    String normalizedRegion = region == null ? "" : region.toLowerCase(Locale.ROOT).trim();
    String normalizedEvent = event == null ? "" : event.toLowerCase(Locale.ROOT).trim();

    List<PlaceInfo> places = fetchPlacesFromDatabase(normalizedKeyword, normalizedRegion, normalizedEvent, minHeat,
        maxHeat, sortBy, limit);
    if (!places.isEmpty() || Files.exists(Path.of(DATABASE_PATH))) {
      return places;
    }
    return fallbackPlaces(normalizedKeyword, normalizedRegion, normalizedEvent, minHeat, maxHeat, sortBy, limit);
  }

  private List<PlaceInfo> fetchPlacesFromDatabase(String keyword, String region, String event, Integer minHeat,
      Integer maxHeat, String sortBy, int limit) {
    if (!Files.exists(Path.of(DATABASE_PATH))) {
      return List.of();
    }

    StringBuilder sql = new StringBuilder();
    sql.append("SELECT a.id, a.name, a.address, a.longitude, a.latitude, ");
    sql.append("COALESCE(r.total_ratings, 0) AS heat_score, ");
    sql.append("COALESCE(r.average_rating, 0) AS average_rating, ");
    sql.append("COALESCE(e.event_name, '') AS event_name ");
    sql.append("FROM attractions a ");
    sql.append("LEFT JOIN (");
    sql.append("  SELECT attraction_id, COUNT(*) AS total_ratings, AVG(rating) AS average_rating ");
    sql.append("  FROM attraction_ratings GROUP BY attraction_id");
    sql.append(") r ON a.id = r.attraction_id ");
    sql.append("LEFT JOIN (");
    sql.append("  SELECT ae.attraction_id, MIN(e.event_name) AS event_name ");
    sql.append("  FROM attraction_events ae ");
    sql.append("  JOIN historical_events e ON ae.event_id = e.id ");
    sql.append("  GROUP BY ae.attraction_id");
    sql.append(") e ON a.id = e.attraction_id ");
    sql.append("WHERE 1=1 ");

    List<Object> params = new ArrayList<>();
    if (!keyword.isBlank()) {
      sql.append("AND (a.name LIKE ? OR a.brief_intro LIKE ? OR a.historical_background LIKE ?) ");
      String value = "%" + keyword + "%";
      params.add(value);
      params.add(value);
      params.add(value);
    }
    if (!region.isBlank()) {
      sql.append("AND a.address LIKE ? ");
      params.add("%" + region + "%");
    }
    if (!event.isBlank()) {
      sql.append("AND e.event_name LIKE ? ");
      params.add("%" + event + "%");
    }
    if (minHeat != null) {
      sql.append("AND COALESCE(r.total_ratings, 0) >= ? ");
      params.add(minHeat);
    }
    if (maxHeat != null) {
      sql.append("AND COALESCE(r.total_ratings, 0) <= ? ");
      params.add(maxHeat);
    }

    if ("rating".equalsIgnoreCase(sortBy)) {
      sql.append("ORDER BY COALESCE(r.average_rating, 0) DESC ");
    } else {
      sql.append("ORDER BY COALESCE(r.total_ratings, 0) DESC ");
    }
    sql.append("LIMIT ? ");
    params.add(limit);

    List<PlaceInfo> places = new ArrayList<>();
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_PATH);
        PreparedStatement statement = connection.prepareStatement(sql.toString())) {
      for (int i = 0; i < params.size(); i++) {
        statement.setObject(i + 1, params.get(i));
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          String address = resultSet.getString("address");
          String regionValue = extractRegion(address);
          places.add(new PlaceInfo(
              resultSet.getLong("id"),
              resultSet.getString("name"),
              address,
              regionValue,
              resultSet.getString("event_name"),
              resultSet.getDouble("longitude"),
              resultSet.getDouble("latitude"),
              resultSet.getInt("heat_score"),
              resultSet.getDouble("average_rating")
          ));
        }
      }
    } catch (SQLException ex) {
      return List.of();
    }

    return places;
  }

  private List<PlaceInfo> fallbackPlaces(String keyword, String region, String event, Integer minHeat, Integer maxHeat,
      String sortBy, int limit) {
    List<PlaceInfo> base = new ArrayList<>();
    base.add(new PlaceInfo(1L, "中共一大会址", "上海市黄浦区兴业路76号", "上海", "建党初期", 121.4752, 31.2204, 9800, 4.8));
    base.add(new PlaceInfo(2L, "南湖红船", "浙江省嘉兴市南湖区", "浙江", "建党初期", 120.7551, 30.7566, 7200, 4.7));
    base.add(new PlaceInfo(3L, "井冈山革命博物馆", "江西省吉安市井冈山市", "江西", "土地革命", 114.1732, 26.5720, 5600, 4.6));
    base.add(new PlaceInfo(4L, "延安革命纪念馆", "陕西省延安市宝塔区", "陕西", "抗日战争", 109.4898, 36.5965, 6100, 4.5));
    base.add(new PlaceInfo(5L, "西柏坡纪念馆", "河北省石家庄市平山县", "河北", "解放战争", 113.9865, 38.3040, 4300, 4.4));

    List<PlaceInfo> filtered = new ArrayList<>();
    for (PlaceInfo place : base) {
      if (!keyword.isBlank() && !matchesKeyword(place, keyword)) {
        continue;
      }
      if (!region.isBlank() && !place.getRegion().toLowerCase(Locale.ROOT).contains(region)) {
        continue;
      }
      if (!event.isBlank() && !place.getEvent().toLowerCase(Locale.ROOT).contains(event)) {
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
    List<PlaceInfo> result = new ArrayList<>();
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

  private java.util.Comparator<PlaceInfo> resolveComparator(String sortBy) {
    if ("rating".equalsIgnoreCase(sortBy)) {
      return java.util.Comparator.comparingDouble(PlaceInfo::getRating).reversed();
    }
    return java.util.Comparator.comparingInt(PlaceInfo::getHeatScore).reversed();
  }

  private String extractRegion(String address) {
    if (address == null || address.isBlank()) {
      return "";
    }
    int provinceIndex = address.indexOf("省");
    if (provinceIndex > 0) {
      return address.substring(0, provinceIndex + 1);
    }
    int cityIndex = address.indexOf("市");
    if (cityIndex > 0) {
      return address.substring(0, cityIndex + 1);
    }
    return address.length() > 2 ? address.substring(0, 2) : address;
  }
}
