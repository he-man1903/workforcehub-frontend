package com.workforce.auth.service;

import com.workforce.auth.domain.AuthUser;
import com.workforce.auth.domain.GoogleIdTokenPayload;
import com.workforce.auth.dto.response.AuthResponse;
import com.workforce.auth.exception.InvalidRefreshTokenException;
import com.workforce.auth.exception.UserDisabledException;
import com.workforce.auth.repository.AuthUserRepository;
import com.workforce.auth.security.GoogleTokenVerifierService;
import com.workforce.auth.security.InternalJwtProvider;
import com.workforce.auth.security.RefreshTokenStore;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Core authentication service.
 *
 * Flow:
 *   1. Frontend obtains a Google ID token via PKCE/OIDC.
 *   2. Frontend POSTs it to POST /auth/google.
 *   3. This service verifies the Google token using Google's public keys.
 *   4. Upserts the user record (create on first login, update on subsequent).
 *   5. Issues a short-lived internal access JWT + long-lived refresh token.
 *   6. Refresh token JTI is stored in Redis for single-use rotation.
 *
 * All tokens are HMAC-SHA256 signed with a shared secret — the same secret
 * is used by the gateway and all downstream services to validate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleTokenVerifierService googleVerifier;
    private final InternalJwtProvider         jwtProvider;
    private final RefreshTokenStore           refreshTokenStore;
    private final AuthUserRepository          userRepository;

    // ── Google login ───────────────────────────────────────────────────────

    @Transactional
    public AuthResponse loginWithGoogle(String rawGoogleIdToken) {
        // Step 1: verify Google token — throws InvalidGoogleTokenException on failure
        GoogleIdTokenPayload googlePayload = googleVerifier.verify(rawGoogleIdToken);
        log.info("Google token verified: subject={}, email={}", googlePayload.getSubject(), googlePayload.getEmail());

        // Step 2: upsert user
        AuthUser user = upsertUser(googlePayload);
        if (!user.isActive()) {
            throw new UserDisabledException("Account is disabled: " + user.getEmail());
        }

        MDC.put("userId", user.getId().toString());
        log.info("Login successful: userId={}, tenantId={}", user.getId(), user.getTenantId());

        // Step 3: issue tokens
        return issueTokens(user);
    }

    // ── Token refresh ──────────────────────────────────────────────────────

    /**
     * Single-use refresh token rotation.
     * Old token is revoked; a new access + refresh pair is issued.
     * If the same refresh token is presented twice, it indicates a replay attack.
     */
    @Transactional
    public AuthResponse refreshAccessToken(String rawRefreshToken) {
        Claims claims = jwtProvider.validateRefreshToken(rawRefreshToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is invalid or expired"));

        String userId = claims.getSubject();
        String jti    = claims.getId();

        // Validate the JTI exists in Redis — proves this token hasn't been rotated or revoked
        Optional<String> storedUserId = refreshTokenStore.getUserId(jti);
        if (storedUserId.isEmpty() || !storedUserId.get().equals(userId)) {
            log.warn("Refresh token JTI not found in Redis — possible replay: userId={}, jti={}", userId, jti);
            throw new InvalidRefreshTokenException("Refresh token has been revoked or already used");
        }

        AuthUser user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new InvalidRefreshTokenException("User not found"));

        if (!user.isActive()) {
            throw new UserDisabledException("Account is disabled: " + user.getEmail());
        }

        // Rotate: revoke old token, issue new pair
        refreshTokenStore.revoke(jti);
        log.info("Refresh token rotated for userId={}", userId);
        return issueTokens(user);
    }

    // ── Logout ─────────────────────────────────────────────────────────────

    /**
     * Revokes the provided refresh token (single device logout).
     * The access token will expire naturally within its TTL (1h).
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        jwtProvider.validateRefreshToken(rawRefreshToken).ifPresent(claims -> {
            refreshTokenStore.revoke(claims.getId());
            log.info("Logged out userId={}, jti={}", claims.getSubject(), claims.getId());
        });
    }

    /**
     * Revokes ALL refresh tokens for a user (logout all devices).
     * Called from the gateway-protected /auth/logout-all endpoint.
     */
    @Transactional
    public void logoutAllDevices(String userId) {
        // RefreshTokenStore.revoke(jti) operates on individual tokens.
        // For revokeAll we need to scan by userId prefix — use Redis SCAN via keys pattern.
        // This is handled in RefreshTokenStore itself.
        log.info("Revoking all sessions for userId={}", userId);
        refreshTokenStore.revokeAllForUser(userId);
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private AuthResponse issueTokens(AuthUser user) {
        String accessToken                        = jwtProvider.generateAccessToken(user);
        InternalJwtProvider.RefreshTokenResult rt = jwtProvider.generateRefreshToken(user);

        // Store refresh token JTI in Redis with matching TTL
        Duration ttl = Duration.ofSeconds(jwtProvider.getRefreshTokenExpirySeconds());
        refreshTokenStore.store(rt.jti(), user.getId().toString(), ttl);

        return AuthResponse.of(
                accessToken,
                rt.token(),
                jwtProvider.getAccessTokenExpirySeconds(),
                user.getTenantId(),
                user.getId().toString(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getPictureUrl()
        );
    }

    private AuthUser upsertUser(GoogleIdTokenPayload p) {
        Optional<AuthUser> existing = userRepository.findByGoogleSubject(p.getSubject());

        if (existing.isPresent()) {
            AuthUser u = existing.get();
            u.setEmail(p.getEmail());
            u.setName(p.getName());
            u.setPictureUrl(p.getPictureUrl());
            u.setLastLoginAt(Instant.now());
            return userRepository.save(u);
        }

        // Derive tenantId: Google Workspace domain if present, else fall back to google subject
        String tenantId = (p.getHostedDomain() != null && !p.getHostedDomain().isBlank())
                ? p.getHostedDomain()
                : "user-" + p.getSubject();

        AuthUser newUser = AuthUser.builder()
                .googleSubject(p.getSubject())
                .email(p.getEmail())
                .name(p.getName())
                .pictureUrl(p.getPictureUrl())
                .tenantId(tenantId)
                .role(AuthUser.Role.USER)
                .active(true)
                .lastLoginAt(Instant.now())
                .build();

        AuthUser saved = userRepository.save(newUser);
        log.info("New user registered: id={}, email={}, tenantId={}", saved.getId(), saved.getEmail(), tenantId);
        return saved;
    }
}
