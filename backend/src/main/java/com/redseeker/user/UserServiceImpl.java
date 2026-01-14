package com.redseeker.user;

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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);
  private static final String INVALID_CREDENTIALS = "invalid username or password";

  private final String databaseUrl;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public UserServiceImpl() {
    this.databaseUrl = resolveDatabaseUrl();
  }

  @Override
  public UserProfileResponse register(UserRegisterRequest request) {
    String username = normalize(request.getUsername());
    String password = request.getPassword();
    if (username == null || password == null) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "username and password are required");
    }
    if (findUserByUsername(username) != null) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "username already exists");
    }
    String hashed = passwordEncoder.encode(password);
    String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, username);
      statement.setString(2, hashed);
      statement.executeUpdate();
      try (PreparedStatement idStatement =
              connection.prepareStatement("SELECT last_insert_rowid()");
          ResultSet keys = idStatement.executeQuery()) {
        if (keys.next()) {
          long userId = keys.getLong(1);
          return getProfile(userId);
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to register user", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to register user");
    }
    throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to register user");
  }

  @Override
  public LoginResponse login(UserLoginRequest request) {
    String username = normalize(request.getUsername());
    String password = request.getPassword();
    if (username == null || password == null) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "username and password are required");
    }

    UserRecord record = findUserByUsername(username);
    if (record == null || !passwordEncoder.matches(password, record.password)) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, INVALID_CREDENTIALS);
    }

    updateLastLogin(record.id);
    UserProfileResponse profile = getProfile(record.id);
    String token = AuthTokenStore.issueToken(record.id);
    return new LoginResponse(token, profile);
  }

  @Override
  public UserProfileResponse getProfile(Long userId) {
    UserRecord record = findUserById(userId);
    if (record == null) {
      throw new ServiceException(ErrorCode.NOT_FOUND, "user not found");
    }
    return new UserProfileResponse(record.id, record.username, record.createdAt, record.lastLogin);
  }

  @Override
  public UserProfileResponse updateProfile(UserProfileUpdateRequest request) {
    UserRecord record = findUserById(request.getUserId());
    if (record == null) {
      throw new ServiceException(ErrorCode.NOT_FOUND, "user not found");
    }

    String username = normalize(request.getUsername());
    String password = request.getPassword();
    boolean updateUsername = username != null && !username.equals(record.username);
    boolean updatePassword = password != null && !password.isBlank();

    if (!updateUsername && !updatePassword) {
      return new UserProfileResponse(record.id, record.username, record.createdAt, record.lastLogin);
    }

    if (updateUsername) {
      UserRecord existing = findUserByUsername(username);
      if (existing != null && !existing.id.equals(record.id)) {
        throw new ServiceException(ErrorCode.VALIDATION_ERROR, "username already exists");
      }
    }

    StringBuilder sql = new StringBuilder("UPDATE users SET ");
    List<Object> params = new ArrayList<>();
    if (updateUsername) {
      sql.append("username = ?");
      params.add(username);
    }
    if (updatePassword) {
      if (!params.isEmpty()) {
        sql.append(", ");
      }
      sql.append("password = ?");
      params.add(passwordEncoder.encode(password));
    }
    sql.append(" WHERE id = ?");
    params.add(record.id);

    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql.toString())) {
      for (int i = 0; i < params.size(); i++) {
        statement.setObject(i + 1, params.get(i));
      }
      statement.executeUpdate();
    } catch (SQLException ex) {
      LOGGER.error("Failed to update profile", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to update profile");
    }
    return getProfile(record.id);
  }

  @Override
  public UserBrowseResponse addBrowse(UserBrowseRequest request) {
    ensureUserExists(request.getUserId());
    String sql = "INSERT INTO user_browse_history (user_id, attraction_id) VALUES (?, ?)";
    try (Connection connection = openConnection();
        PreparedStatement statement =
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      statement.setLong(1, request.getUserId());
      statement.setLong(2, request.getAttractionId());
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) {
          long id = keys.getLong(1);
          return getBrowseById(id, request.getUserId());
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to add browse history", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to add browse history");
    }
    throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to add browse history");
  }

  @Override
  public List<UserBrowseResponse> listBrowse(Long userId) {
    ensureUserExists(userId);
    String sql =
        "SELECT id, user_id, attraction_id, browse_time FROM user_browse_history "
            + "WHERE user_id = ? ORDER BY browse_time DESC";
    List<UserBrowseResponse> results = new ArrayList<>();
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, userId);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          results.add(
              new UserBrowseResponse(
                  rs.getLong("id"),
                  rs.getLong("user_id"),
                  rs.getLong("attraction_id"),
                  rs.getString("browse_time")));
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to list browse history", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to list browse history");
    }
    return results;
  }

  @Override
  public UserBrowseResponse updateBrowse(UserBrowseUpdateRequest request) {
    ensureUserExists(request.getUserId());
    String sql;
    boolean hasBrowseTime = request.getBrowseTime() != null && !request.getBrowseTime().isBlank();
    if (hasBrowseTime) {
      sql = "UPDATE user_browse_history SET attraction_id = ?, browse_time = ? WHERE id = ? AND user_id = ?";
    } else {
      sql = "UPDATE user_browse_history SET attraction_id = ? WHERE id = ? AND user_id = ?";
    }
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      int index = 1;
      statement.setLong(index++, request.getAttractionId());
      if (hasBrowseTime) {
        statement.setString(index++, request.getBrowseTime());
      }
      statement.setLong(index++, request.getId());
      statement.setLong(index, request.getUserId());
      int affected = statement.executeUpdate();
      if (affected == 0) {
        throw new ServiceException(ErrorCode.NOT_FOUND, "browse record not found");
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to update browse history", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to update browse history");
    }
    return getBrowseById(request.getId(), request.getUserId());
  }

  @Override
  public void deleteBrowse(Long id, Long userId) {
    ensureUserExists(userId);
    String sql = "DELETE FROM user_browse_history WHERE id = ? AND user_id = ?";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, id);
      statement.setLong(2, userId);
      int affected = statement.executeUpdate();
      if (affected == 0) {
        throw new ServiceException(ErrorCode.NOT_FOUND, "browse record not found");
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to delete browse history", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to delete browse history");
    }
  }

  @Override
  public UserRatingResponse addRating(UserRatingRequest request) {
    ensureUserExists(request.getUserId());
    String sql =
        "INSERT INTO attraction_ratings (attraction_id, user_id, rating, comment) VALUES (?, ?, ?, ?)";
    try (Connection connection = openConnection();
        PreparedStatement statement =
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      statement.setLong(1, request.getAttractionId());
      statement.setLong(2, request.getUserId());
      statement.setInt(3, request.getRating());
      statement.setString(4, request.getComment());
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) {
          long id = keys.getLong(1);
          return getRatingById(id, request.getUserId());
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to add rating", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to add rating");
    }
    throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to add rating");
  }

  @Override
  public List<UserRatingResponse> listRatings(Long userId) {
    ensureUserExists(userId);
    String sql =
        "SELECT id, user_id, attraction_id, rating, comment, created_at "
            + "FROM attraction_ratings WHERE user_id = ? ORDER BY created_at DESC";
    List<UserRatingResponse> results = new ArrayList<>();
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, userId);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          results.add(
              new UserRatingResponse(
                  rs.getLong("id"),
                  rs.getLong("user_id"),
                  rs.getLong("attraction_id"),
                  rs.getInt("rating"),
                  rs.getString("comment"),
                  rs.getString("created_at")));
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to list ratings", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to list ratings");
    }
    return results;
  }

  @Override
  public UserRatingResponse updateRating(UserRatingUpdateRequest request) {
    ensureUserExists(request.getUserId());
    String sql = "UPDATE attraction_ratings SET rating = ?, comment = ? WHERE id = ? AND user_id = ?";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, request.getRating());
      statement.setString(2, request.getComment());
      statement.setLong(3, request.getId());
      statement.setLong(4, request.getUserId());
      int affected = statement.executeUpdate();
      if (affected == 0) {
        throw new ServiceException(ErrorCode.NOT_FOUND, "rating record not found");
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to update rating", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to update rating");
    }
    return getRatingById(request.getId(), request.getUserId());
  }

  @Override
  public void deleteRating(Long id, Long userId) {
    ensureUserExists(userId);
    String sql = "DELETE FROM attraction_ratings WHERE id = ? AND user_id = ?";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, id);
      statement.setLong(2, userId);
      int affected = statement.executeUpdate();
      if (affected == 0) {
        throw new ServiceException(ErrorCode.NOT_FOUND, "rating record not found");
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to delete rating", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to delete rating");
    }
  }

  @Override
  public void assertAuthorized(String token, Long userId) {
    Long tokenUserId = AuthTokenStore.resolveUserId(token);
    if (tokenUserId == null || !tokenUserId.equals(userId)) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "invalid or missing auth token");
    }
  }

  @Override
  public Long resolveUserId(String token) {
    return AuthTokenStore.resolveUserId(token);
  }

  private UserBrowseResponse getBrowseById(Long id, Long userId) {
    String sql =
        "SELECT id, user_id, attraction_id, browse_time FROM user_browse_history "
            + "WHERE id = ? AND user_id = ?";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, id);
      statement.setLong(2, userId);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          return new UserBrowseResponse(
              rs.getLong("id"),
              rs.getLong("user_id"),
              rs.getLong("attraction_id"),
              rs.getString("browse_time"));
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to fetch browse history", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to fetch browse history");
    }
    throw new ServiceException(ErrorCode.NOT_FOUND, "browse record not found");
  }

  private UserRatingResponse getRatingById(Long id, Long userId) {
    String sql =
        "SELECT id, user_id, attraction_id, rating, comment, created_at "
            + "FROM attraction_ratings WHERE id = ? AND user_id = ?";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, id);
      statement.setLong(2, userId);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          return new UserRatingResponse(
              rs.getLong("id"),
              rs.getLong("user_id"),
              rs.getLong("attraction_id"),
              rs.getInt("rating"),
              rs.getString("comment"),
              rs.getString("created_at"));
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to fetch rating", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to fetch rating");
    }
    throw new ServiceException(ErrorCode.NOT_FOUND, "rating record not found");
  }

  private void updateLastLogin(Long userId) {
    String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, userId);
      statement.executeUpdate();
    } catch (SQLException ex) {
      LOGGER.warn("Failed to update last_login for user {}", userId, ex);
    }
  }

  private void ensureUserExists(Long userId) {
    if (findUserById(userId) == null) {
      throw new ServiceException(ErrorCode.NOT_FOUND, "user not found");
    }
  }

  private UserRecord findUserById(Long userId) {
    if (userId == null) {
      return null;
    }
    String sql = "SELECT id, username, password, created_at, last_login FROM users WHERE id = ?";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, userId);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          return new UserRecord(
              rs.getLong("id"),
              rs.getString("username"),
              rs.getString("password"),
              rs.getString("created_at"),
              rs.getString("last_login"));
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to query user", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to query user");
    }
    return null;
  }

  private UserRecord findUserByUsername(String username) {
    if (username == null || username.isBlank()) {
      return null;
    }
    String sql = "SELECT id, username, password, created_at, last_login FROM users WHERE username = ?";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, username);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          return new UserRecord(
              rs.getLong("id"),
              rs.getString("username"),
              rs.getString("password"),
              rs.getString("created_at"),
              rs.getString("last_login"));
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to query user by username", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to query user");
    }
    return null;
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

  private String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static final class UserRecord {
    private final Long id;
    private final String username;
    private final String password;
    private final String createdAt;
    private final String lastLogin;

    private UserRecord(Long id, String username, String password, String createdAt, String lastLogin) {
      this.id = id;
      this.username = username;
      this.password = password;
      this.createdAt = createdAt;
      this.lastLogin = lastLogin;
    }
  }
}
