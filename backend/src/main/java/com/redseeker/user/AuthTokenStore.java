package com.redseeker.user;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class AuthTokenStore {
  private static final Map<String, TokenInfo> TOKENS = new ConcurrentHashMap<>();
  private static final Map<Long, String> USER_TOKENS = new ConcurrentHashMap<>();

  private AuthTokenStore() {
  }

  static String issueToken(Long userId) {
    String token = UUID.randomUUID().toString();
    String previous = USER_TOKENS.put(userId, token);
    if (previous != null) {
      TOKENS.remove(previous);
    }
    TOKENS.put(token, new TokenInfo(userId, Instant.now().toString()));
    return token;
  }

  static Long resolveUserId(String token) {
    if (token == null || token.isBlank()) {
      return null;
    }
    TokenInfo info = TOKENS.get(token);
    return info == null ? null : info.userId;
  }

  private static final class TokenInfo {
    private final Long userId;
    private final String issuedAt;

    private TokenInfo(Long userId, String issuedAt) {
      this.userId = userId;
      this.issuedAt = issuedAt;
    }
  }
}
