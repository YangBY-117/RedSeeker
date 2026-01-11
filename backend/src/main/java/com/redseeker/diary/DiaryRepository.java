package com.redseeker.diary;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DiaryRepository {
  private static final String DATABASE_PATH = "database/red_tourism.db";
  private static final List<String> TABLE_CANDIDATES = List.of(
      "diary_entries",
      "diary",
      "study_diary",
      "diaries",
      "travel_diary"
  );

  private final String tableName;

  public DiaryRepository() {
    this.tableName = resolveTableName().orElse(null);
  }

  public boolean isDatabaseReady() {
    return tableName != null;
  }

  public DiaryEntry insert(DiaryEntry entry) {
    String sql = "INSERT INTO " + tableName
        + " (title, content, images, tags, attraction_id, checked_in, check_in_note, template, shared, created_at, updated_at)"
        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_PATH);
        PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      statement.setString(1, entry.getTitle());
      statement.setString(2, entry.getContent());
      statement.setString(3, String.join("|", entry.getImages()));
      statement.setString(4, String.join("|", entry.getTags()));
      if (entry.getAttractionId() == null) {
        statement.setObject(5, null);
      } else {
        statement.setLong(5, entry.getAttractionId());
      }
      statement.setInt(6, entry.isCheckedIn() ? 1 : 0);
      statement.setString(7, entry.getCheckInNote());
      statement.setString(8, entry.getTemplate());
      statement.setInt(9, entry.isShared() ? 1 : 0);
      statement.setString(10, entry.getCreatedAt().toString());
      statement.setString(11, entry.getUpdatedAt().toString());
      statement.executeUpdate();

      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) {
          return fetchById(keys.getLong(1)).orElse(entry);
        }
      }
      return entry;
    } catch (SQLException ex) {
      return entry;
    }
  }

  public DiaryEntry update(DiaryEntry entry) {
    String sql = "UPDATE " + tableName
        + " SET title = ?, content = ?, images = ?, tags = ?, attraction_id = ?, checked_in = ?, check_in_note = ?,"
        + " template = ?, shared = ?, updated_at = ? WHERE id = ?";
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_PATH);
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, entry.getTitle());
      statement.setString(2, entry.getContent());
      statement.setString(3, String.join("|", entry.getImages()));
      statement.setString(4, String.join("|", entry.getTags()));
      if (entry.getAttractionId() == null) {
        statement.setObject(5, null);
      } else {
        statement.setLong(5, entry.getAttractionId());
      }
      statement.setInt(6, entry.isCheckedIn() ? 1 : 0);
      statement.setString(7, entry.getCheckInNote());
      statement.setString(8, entry.getTemplate());
      statement.setInt(9, entry.isShared() ? 1 : 0);
      statement.setString(10, entry.getUpdatedAt().toString());
      statement.setLong(11, entry.getId());
      statement.executeUpdate();
      return fetchById(entry.getId()).orElse(entry);
    } catch (SQLException ex) {
      return entry;
    }
  }

  public Optional<DiaryEntry> fetchById(Long id) {
    String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_PATH);
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return Optional.of(mapEntry(resultSet));
        }
      }
    } catch (SQLException ex) {
      return Optional.empty();
    }
    return Optional.empty();
  }

  public List<DiaryEntry> list() {
    String sql = "SELECT * FROM " + tableName + " ORDER BY datetime(created_at) DESC";
    List<DiaryEntry> entries = new ArrayList<>();
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_PATH);
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        entries.add(mapEntry(resultSet));
      }
    } catch (SQLException ex) {
      return Collections.emptyList();
    }
    return entries;
  }

  private DiaryEntry mapEntry(ResultSet resultSet) throws SQLException {
    return new DiaryEntry(
        resultSet.getLong("id"),
        resultSet.getString("title"),
        resultSet.getString("content"),
        parseList(resultSet.getString("images")),
        parseList(resultSet.getString("tags")),
        readLong(resultSet, "attraction_id"),
        resultSet.getInt("checked_in") == 1,
        resultSet.getString("check_in_note"),
        resultSet.getString("template"),
        resultSet.getInt("shared") == 1,
        Instant.parse(resultSet.getString("created_at")),
        Instant.parse(resultSet.getString("updated_at"))
    );
  }

  private List<String> parseList(String text) {
    if (text == null || text.isBlank()) {
      return new ArrayList<>();
    }
    return new ArrayList<>(Arrays.asList(text.split("\\|")));
  }

  private Long readLong(ResultSet resultSet, String column) throws SQLException {
    long value = resultSet.getLong(column);
    return resultSet.wasNull() ? null : value;
  }

  private Optional<String> resolveTableName() {
    if (!Files.exists(Path.of(DATABASE_PATH))) {
      return Optional.empty();
    }
    String sql = "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?";
    for (String candidate : TABLE_CANDIDATES) {
      try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_PATH);
          PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, candidate);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return Optional.of(candidate);
          }
        }
      } catch (SQLException ex) {
        return Optional.empty();
      }
    }
    return Optional.empty();
  }
}
