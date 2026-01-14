package com.redseeker.place;

import com.redseeker.common.ErrorCode;
import com.redseeker.common.ServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PlaceServiceImpl implements PlaceService {
  private static final Logger LOGGER = LoggerFactory.getLogger(PlaceServiceImpl.class);

  private final String databaseUrl;

  public PlaceServiceImpl() {
    this.databaseUrl = resolveDatabaseUrl();
  }

  @Override
  public List<Place> queryPlaces(PlaceQueryCondition condition) {
    if (condition == null) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "condition is required");
    }
    String province = normalize(condition.getProvince());
    String period = normalize(condition.getHistoricalPeriod());
    List<String> keywords = normalizeKeywords(condition.getKeywords());

    List<PlaceRecord> records = loadPlaceRecords(province, period, keywords);
    return records.stream()
        .map(record -> new Place(
            record.id,
            record.name,
            record.address,
            formatCategory(record.categoryId),
            record.history,
            new ArrayList<>(record.periods)))
        .collect(Collectors.toList());
  }

  private List<PlaceRecord> loadPlaceRecords(String province, String period, List<String> keywords) {
    StringBuilder sql = new StringBuilder(
        "SELECT a.id, a.name, a.address, a.category, a.brief_intro, a.historical_background, "
            + "he.period "
            + "FROM attractions a "
            + "LEFT JOIN attraction_events ae ON ae.attraction_id = a.id "
            + "LEFT JOIN historical_events he ON he.id = ae.event_id "
            + "WHERE 1=1");

    List<Object> params = new ArrayList<>();
    if (province != null) {
      sql.append(" AND (a.address LIKE ? OR a.name LIKE ?)");
      String provincePattern = "%" + province + "%";
      params.add(provincePattern);
      params.add(provincePattern);
    }
    if (period != null) {
      sql.append(" AND he.period LIKE ?");
      params.add("%" + period + "%");
    }
    if (!keywords.isEmpty()) {
      sql.append(" AND (");
      for (int i = 0; i < keywords.size(); i++) {
        if (i > 0) {
          sql.append(" OR ");
        }
        sql.append("a.name LIKE ? OR a.brief_intro LIKE ? OR a.historical_background LIKE ?");
        String keywordPattern = "%" + keywords.get(i) + "%";
        params.add(keywordPattern);
        params.add(keywordPattern);
        params.add(keywordPattern);
      }
      sql.append(")");
    }

    Map<String, PlaceRecord> recordMap = new HashMap<>();
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql.toString())) {
      for (int i = 0; i < params.size(); i++) {
        statement.setObject(i + 1, params.get(i));
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          String id = String.valueOf(resultSet.getInt("id"));
          PlaceRecord record = recordMap.get(id);
          if (record == null) {
            record = new PlaceRecord(
                id,
                resultSet.getString("name"),
                resultSet.getString("address"),
                resultSet.getInt("category"),
                pickHistory(resultSet.getString("historical_background"),
                    resultSet.getString("brief_intro")));
            recordMap.put(id, record);
          }
          String periodValue = resultSet.getString("period");
          if (periodValue != null && !periodValue.isBlank()) {
            record.periods.add(periodValue.trim());
          }
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to query places", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to query places");
    }
    return new ArrayList<>(recordMap.values());
  }

  private String pickHistory(String history, String briefIntro) {
    if (history != null && !history.isBlank()) {
      return history;
    }
    if (briefIntro != null && !briefIntro.isBlank()) {
      return briefIntro;
    }
    return "";
  }

  private String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isBlank() ? null : trimmed;
  }

  private List<String> normalizeKeywords(List<String> keywords) {
    if (keywords == null) {
      return List.of();
    }
    return keywords.stream()
        .filter(value -> value != null && !value.isBlank())
        .map(String::trim)
        .collect(Collectors.toList());
  }

  private String formatCategory(int categoryId) {
    switch (categoryId) {
      case 1:
        return "革命旧址";
      case 2:
        return "名人故居";
      case 3:
        return "纪念馆";
      case 4:
        return "烈士陵园";
      case 5:
        return "爱国主义教育基地";
      default:
        return "Category-" + categoryId;
    }
  }

  private Connection openConnection() throws SQLException {
    return DriverManager.getConnection(databaseUrl);
  }

  private String resolveDatabaseUrl() {
    String override = System.getenv("REDSEEKER_DB_PATH");
    if (override != null && !override.isBlank()) {
      return "jdbc:sqlite:" + override;
    }
    Path direct = Paths.get("database", "red_tourism.db");
    if (Files.exists(direct)) {
      return "jdbc:sqlite:" + direct.toAbsolutePath();
    }
    Path parent = Paths.get("..", "database", "red_tourism.db");
    if (Files.exists(parent)) {
      return "jdbc:sqlite:" + parent.toAbsolutePath();
    }
    return "jdbc:sqlite:database/red_tourism.db";
  }

  private static final class PlaceRecord {
    private final String id;
    private final String name;
    private final String address;
    private final int categoryId;
    private final String history;
    private final Set<String> periods = new HashSet<>();

    private PlaceRecord(String id, String name, String address, int categoryId, String history) {
      this.id = id;
      this.name = name;
      this.address = address;
      this.categoryId = categoryId;
      this.history = history;
    }
  }
}
