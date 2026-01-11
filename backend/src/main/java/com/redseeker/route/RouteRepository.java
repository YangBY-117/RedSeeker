package com.redseeker.route;

import com.redseeker.common.ErrorCode;
import com.redseeker.common.ServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class RouteRepository {
  private static final String DATABASE_PATH = "database/red_tourism.db";

  public AttractionInfo getAttraction(Long id) {
    List<AttractionInfo> attractions = getAttractions(List.of(id));
    if (attractions.isEmpty()) {
      throw new ServiceException(ErrorCode.NOT_FOUND, "景点不存在");
    }
    return attractions.get(0);
  }

  public List<AttractionInfo> getAttractions(List<Long> ids) {
    if (!Files.exists(Path.of(DATABASE_PATH))) {
      return fallbackAttractions(ids);
    }

    String placeholders = String.join(",", ids.stream().map(value -> "?").toList());
    String sql = "SELECT a.id, a.name, a.address, a.longitude, a.latitude, "
        + "COALESCE(e.period, '') AS period, COALESCE(e.start_year, 9999) AS start_year, "
        + "COALESCE(e.event_name, '') AS event_name "
        + "FROM attractions a "
        + "LEFT JOIN ("
        + "  SELECT ae.attraction_id, MIN(e.start_year) AS start_year, MIN(e.period) AS period, "
        + "  GROUP_CONCAT(e.event_name, '|') AS event_name "
        + "  FROM attraction_events ae "
        + "  JOIN historical_events e ON ae.event_id = e.id "
        + "  GROUP BY ae.attraction_id"
        + ") e ON a.id = e.attraction_id "
        + "WHERE a.id IN (" + placeholders + ")";

    Map<Long, AttractionInfo> result = new HashMap<>();
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_PATH);
        PreparedStatement statement = connection.prepareStatement(sql)) {
      int index = 1;
      for (Long id : ids) {
        statement.setLong(index++, id);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          result.put(resultSet.getLong("id"), mapAttraction(resultSet));
        }
      }
    } catch (SQLException ex) {
      return fallbackAttractions(ids);
    }

    List<AttractionInfo> attractions = new ArrayList<>();
    for (Long id : ids) {
      AttractionInfo attraction = result.get(id);
      if (attraction != null) {
        attractions.add(attraction);
      }
    }
    return attractions;
  }

  private AttractionInfo mapAttraction(ResultSet resultSet) throws SQLException {
    String events = resultSet.getString("event_name");
    List<String> storyPoints = new ArrayList<>();
    if (events != null && !events.isBlank()) {
      for (String event : events.split("\\|")) {
        if (!event.isBlank()) {
          storyPoints.add(event);
        }
      }
    }

    return new AttractionInfo(
        resultSet.getLong("id"),
        resultSet.getString("name"),
        resultSet.getString("address"),
        resultSet.getDouble("longitude"),
        resultSet.getDouble("latitude"),
        resultSet.getString("period"),
        storyPoints,
        resultSet.getInt("start_year")
    );
  }

  private List<AttractionInfo> fallbackAttractions(List<Long> ids) {
    Map<Long, AttractionInfo> fallback = Map.of(
        1L, new AttractionInfo(1L, "中共一大会址", "上海市黄浦区兴业路76号", 121.4752, 31.2204,
            "建党初期", List.of("初心教育", "建党会议场景"), 1921),
        2L, new AttractionInfo(2L, "南湖红船", "浙江省嘉兴市南湖区", 120.7551, 30.7566,
            "建党初期", List.of("红船精神", "水上课堂"), 1921),
        3L, new AttractionInfo(3L, "井冈山革命博物馆", "江西省吉安市井冈山市", 114.1732, 26.5720,
            "土地革命", List.of("井冈山斗争", "革命根据地"), 1927),
        4L, new AttractionInfo(4L, "延安革命纪念馆", "陕西省延安市宝塔区", 109.4898, 36.5965,
            "抗日战争", List.of("延安精神", "窑洞课堂"), 1935)
    );

    List<AttractionInfo> data = new ArrayList<>();
    for (Long id : ids) {
      AttractionInfo attraction = fallback.get(id);
      if (attraction != null) {
        data.add(attraction);
      }
    }
    return data;
  }
}
