package com.workforce.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Stores and validates refresh token metadata in Redis.
 *
 * Key scheme:
 *   refresh:{jti}          → userId       (primary lookup: jti → owner)
 *   refresh:user:{userId}  → Set of JTIs  (reverse index: user → all active JTIs)
 *
 * Single-use rotation:
 *   On every /auth/refresh call, the old JTI is deleted and a new one is stored.
 *   If the same JTI is presented twice, it's absent from Redis → replay rejected.
 *
 * Logout all devices:
 *   All JTIs for a user are found via the reverse index and deleted.
 *
 * Redis is the ONLY state in this stateless architecture — all app servers share it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String JTI_PREFIX  = "refresh:";
    private static final String USER_PREFIX = "refresh:user:";
    private static final String DENY_PREFIX = "deny:";

    private final StringRedisTemplate redis;

    /**
     * Store a new refresh token JTI with the given TTL.
     *
     * @param jti    unique token ID from the JWT's `jti` claim
     * @param userId internal user UUID (as string)
     * @param ttl    time-to-live, matching the JWT's expiry
     */
    public void store(String jti, String userId, Duration ttl) {
        // Primary: jti → userId
        redis.opsForValue().set(JTI_PREFIX + jti, userId, ttl);

        // Reverse index: userId → jti set (for revokeAll support)
        redis.opsForSet().add(USER_PREFIX + userId, jti);
        redis.expire(USER_PREFIX + userId, ttl.plusMinutes(5)); // slightly longer than token TTL

        log.debug("Stored refresh token: jti={}, userId={}, ttl={}", jti, userId, ttl);
    }

    /**
     * Returns the userId associated with a JTI if it exists and hasn't been revoked.
     */
    public Optional<String> getUserId(String jti) {
        String value = redis.opsForValue().get(JTI_PREFIX + jti);
        return Optional.ofNullable(value);
    }

    /**
     * Revoke a single refresh token by JTI.
     */
    public void revoke(String jti) {
        // Look up userId to clean reverse index before deleting primary key
        String userId = redis.opsForValue().get(JTI_PREFIX + jti);
        redis.delete(JTI_PREFIX + jti);
        if (userId != null) {
            redis.opsForSet().remove(USER_PREFIX + userId, jti);
        }
        log.debug("Revoked refresh token: jti={}", jti);
    }

    /**
     * Revoke ALL refresh tokens for a given user (logout all devices).
     */
    public void revokeAllForUser(String userId) {
        Set<String> jtis = redis.opsForSet().members(USER_PREFIX + userId);
        if (jtis != null && !jtis.isEmpty()) {
            // Delete all primary keys
            jtis.forEach(jti -> redis.delete(JTI_PREFIX + jti));
            log.info("Revoked {} refresh token(s) for userId={}", jtis.size(), userId);
        }
        // Delete the reverse index
        redis.delete(USER_PREFIX + userId);
    }

    // ── Access token deny-list (instant revocation on logout before expiry) ──

    /**
     * Add an access token's JTI to the deny-list.
     * Used when a user logs out with a still-valid access token.
     */
    public void denyAccessToken(String jti, Duration remainingTtl) {
        redis.opsForValue().set(DENY_PREFIX + jti, "1", remainingTtl);
        log.debug("Access token added to deny-list: jti={}", jti);
    }

    public boolean isAccessTokenDenied(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(DENY_PREFIX + jti));
    }
}
