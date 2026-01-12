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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
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
  private static final String IMAGES_TABLE = "diary_images";
  private static final DateTimeFormatter SQLITE_DATE_FORMATTER = new DateTimeFormatterBuilder()
      .appendPattern("yyyy-MM-dd HH:mm:ss")
      .optionalStart()
      .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
      .optionalEnd()
      .toFormatter();

  private final String tableName;
  private final boolean hasImagesTable;

  public DiaryRepository() {
    this.tableName = resolveTableName().orElse(null);
    this.hasImagesTable = resolveImagesTable();
  }

  public boolean isDatabaseReady() {
    return tableName != null;
  }

  public DiaryEntry insert(DiaryEntry entry) {
    String sql = "INSERT INTO " + tableName
        + " (user_id, attraction_id, title, content, is_public, created_at, updated_at)"
        + " VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_PATH);
        PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      statement.setLong(1, entry.getUserId());
      if (entry.getAttractionId() == null) {
        statement.setObject(2, null);
      } else {
        statement.setLong(2, entry.getAttractionId());
      }
      statement.setString(3, entry.getTitle());
      statement.setString(4, entry.getContent());
      statement.setInt(5, entry.isPublicEntry() ? 1 : 0);
      statement.setString(6, entry.getCreatedAt().toString());
      statement.setString(7, entry.getUpdatedAt().toString());
      statement.executeUpdate();

      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) {
          long id = keys.getLong(1);
          replaceImages(connection, id, entry.getImages());
          return fetchById(id).orElse(entry);
        }
      }
      return entry;
    } catch (SQLException ex) {
      return entry;
    }
  }

  public DiaryEntry update(DiaryEntry entry) {
    String sql = "UPDATE " + tableName
        + " SET user_id = ?, attraction_id = ?, title = ?, content = ?, is_public = ?, updated_at = ? WHERE id = ?";
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_PATH);
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, entry.getUserId());
      if (entry.getAttractionId() == null) {
        statement.setObject(2, null);
      } else {
        statement.setLong(2, entry.getAttractionId());
      }
      statement.setString(3, entry.getTitle());
      statement.setString(4, entry.getContent());
      statement.setInt(5, entry.isPublicEntry() ? 1 : 0);
      statement.setString(6, entry.getUpdatedAt().toString());
      statement.setLong(7, entry.getId());
      statement.executeUpdate();
      replaceImages(connection, entry.getId(), entry.getImages());
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
    Long id = resultSet.getLong("id");
    return new DiaryEntry(
        id,
        resultSet.getLong("user_id"),
        resultSet.getString("title"),
        resultSet.getString("content"),
        fetchImages(id),
        readLong(resultSet, "attraction_id"),
        resultSet.getInt("is_public") == 1,
        parseInstant(resultSet.getString("created_at")),
        parseInstant(resultSet.getString("updated_at"))
    );
  }

  private Long readLong(ResultSet resultSet, String column) throws SQLException {
    long value = resultSet.getLong(column);
    return resultSet.wasNull() ? null : value;
  }

  private Instant parseInstant(String value) {
    if (value == null || value.isBlank()) {
      return Instant.now();
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException ex) {
      LocalDateTime local = LocalDateTime.parse(value, SQLITE_DATE_FORMATTER);
      return local.atZone(ZoneId.systemDefault()).toInstant();
    }
  }

  private List<String> fetchImages(Long diaryId) {
    if (!hasImagesTable) {
      return new ArrayList<>();
    }
    String sql = "SELECT file_path FROM " + IMAGES_TABLE + " WHERE diary_id = ? ORDER BY display_order, id";
    List<String> images = new ArrayList<>();
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_PATH);
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, diaryId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          String path = resultSet.getString("file_path");
          if (path != null && !path.isBlank()) {
            images.add(path);
          }
        }
      }
    } catch (SQLException ex) {
      return new ArrayList<>();
    }
    return images;
  }

  private void replaceImages(Connection connection, Long diaryId, List<String> images) throws SQLException {
    if (!hasImagesTable) {
      return;
    }
    try (PreparedStatement delete = connection.prepareStatement(
        "DELETE FROM " + IMAGES_TABLE + " WHERE diary_id = ?")) {
      delete.setLong(1, diaryId);
      delete.executeUpdate();
    }
    if (images == null || images.isEmpty()) {
      return;
    }
    String insertSql = "INSERT INTO " + IMAGES_TABLE + " (diary_id, file_path, display_order) VALUES (?, ?, ?)";
    try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
      int order = 0;
      for (String image : images) {
        if (image == null || image.isBlank()) {
          continue;
        }
        insert.setLong(1, diaryId);
        insert.setString(2, image);
        insert.setInt(3, order);
        insert.addBatch();
        order += 1;
      }
      insert.executeBatch();
    }
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

  private boolean resolveImagesTable() {
    if (!Files.exists(Path.of(DATABASE_PATH))) {
      return false;
    }
    String sql = "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?";
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_PATH);
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, IMAGES_TABLE);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next();
      }
    } catch (SQLException ex) {
      return false;
    }
  }
}
