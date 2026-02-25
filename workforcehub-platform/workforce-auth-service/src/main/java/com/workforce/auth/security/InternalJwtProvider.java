package com.workforce.auth.security;

import com.workforce.auth.config.JwtProperties;
import com.workforce.auth.domain.AuthUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages the internal JWT lifecycle:
 * - Access tokens  (short-lived, 1h, stateless)
 * - Refresh tokens (long-lived, 7d, tracked in Redis for revocation)
 *
 * Claims in access token:
 *   sub       → user UUID (internal)
 *   email     → user email
 *   tenantId  → tenant identifier (used by gateway to scope DB queries)
 *   role      → user role
 *   googleSub → Google subject (for audit)
 *   type      → "access"
 *
 * Claims in refresh token:
 *   sub   → user UUID
 *   jti   → unique token ID (stored in Redis, used for revocation)
 *   type  → "refresh"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InternalJwtProvider {

    private final JwtProperties jwtProperties;

    private SecretKey signingKey() {
        byte[] bytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }

    // ── Access token ────────────────────────────────────────────────────────

    public String generateAccessToken(AuthUser user) {
        Date now     = new Date();
        Date expiry  = new Date(now.getTime() + jwtProperties.getExpirationMs());

        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiry)
                .claim("email",     user.getEmail())
                .claim("name",      user.getName())
                .claim("tenantId",  user.getTenantId())
                .claim("role",      user.getRole().name())
                .claim("googleSub", user.getGoogleSubject())
                .claim("type",      "access")
                .signWith(signingKey())
                .compact();
    }

    // ── Refresh token ───────────────────────────────────────────────────────

    /**
     * Generates a refresh token. The returned JTI must be stored in Redis
     * (by the caller) to enable revocation.
     */
    public RefreshTokenResult generateRefreshToken(AuthUser user) {
        String jti  = UUID.randomUUID().toString();
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshExpirationMs());

        String token = Jwts.builder()
                .subject(user.getId().toString())
                .issuer(jwtProperties.getIssuer())
                .id(jti)
                .issuedAt(now)
                .expiration(expiry)
                .claim("type", "refresh")
                .signWith(signingKey())
                .compact();

        return new RefreshTokenResult(token, jti, expiry);
    }

    // ── Validation ──────────────────────────────────────────────────────────

    public Optional<Claims> validateAccessToken(String token) {
        return parse(token, "access");
    }

    public Optional<Claims> validateRefreshToken(String token) {
        return parse(token, "refresh");
    }

    private Optional<Claims> parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .requireIssuer(jwtProperties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!expectedType.equals(claims.get("type", String.class))) {
                log.warn("JWT type mismatch: expected='{}' got='{}'", expectedType, claims.get("type"));
                return Optional.empty();
            }
            return Optional.of(claims);

        } catch (ExpiredJwtException e) {
            log.debug("JWT expired");
        } catch (MalformedJwtException | SecurityException e) {
            log.warn("JWT invalid: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("JWT parse error: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public long getAccessTokenExpirySeconds() {
        return jwtProperties.getExpirationMs() / 1000;
    }

    public long getRefreshTokenExpirySeconds() {
        return jwtProperties.getRefreshExpirationMs() / 1000;
    }

    // ── Result types ─────────────────────────────────────────────────────────

    public record RefreshTokenResult(String token, String jti, Date expiry) {}
}
