package com.workforce.auth.controller;

import com.workforce.auth.dto.request.GoogleTokenRequest;
import com.workforce.auth.dto.request.LogoutRequest;
import com.workforce.auth.dto.request.RefreshTokenRequest;
import com.workforce.auth.dto.response.AuthResponse;
import com.workforce.auth.config.AppProperties;
import com.workforce.auth.exception.InvalidGoogleTokenException;
import com.workforce.auth.service.AuthService;
import com.workforce.auth.service.GoogleOAuthFlowService;
import com.workforce.auth.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

/**
 * Auth endpoints — all public (no JWT required to call /google, /refresh,
 * /logout).
 * /logout-all requires X-User-Id header (set by gateway from the access token).
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Google OAuth2 token exchange and JWT lifecycle")
public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;
    private final GoogleOAuthFlowService googleOAuthFlowService;
    private final AppProperties appProperties;

    /**
     * GET /auth/login/google
     * Redirects the user to Google sign-in. After sign-in, Google redirects to
     * /auth/oauth2/callback.
     * The backend exchanges the code (with client_secret) and redirects to the
     * frontend with tokens.
     */
    @GetMapping("/login/google")
    @Operation(summary = "Start server-side Google OAuth2 flow")
    public ResponseEntity<Void> loginGoogle(HttpServletRequest httpRequest) {
        rateLimitService.checkRateLimit(getClientIp(httpRequest));
        String redirectUrl = googleOAuthFlowService.buildLoginRedirectUrl();
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
    }

    /**
     * GET /auth/oauth2/callback
     * Google redirects here with ?code=...&state=... . We exchange the code for
     * tokens and redirect to frontend.
     */
    @GetMapping("/oauth2/callback")
    @Operation(summary = "Google OAuth2 callback — exchange code and redirect to frontend")
    public ResponseEntity<Void> oauth2Callback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            HttpServletRequest httpRequest) {
        rateLimitService.checkRateLimit(getClientIp(httpRequest));
        try {
            AuthResponse response = googleOAuthFlowService.handleCallback(code, state);
            URI frontendUri = googleOAuthFlowService.getFrontendCallbackUri(
                    response.accessToken(), response.refreshToken());
            return ResponseEntity.status(HttpStatus.FOUND).location(frontendUri).build();
        } catch (InvalidGoogleTokenException e) {
            log.warn("OAuth2 callback failed: {}", e.getMessage());
            String base = appProperties.getFrontendUrl().replaceAll("/$", "");
            URI loginUri = URI.create(base + "/login?error=oauth_failed");
            return ResponseEntity.status(HttpStatus.FOUND).location(loginUri).build();
        }
    }

    /**
     * POST /auth/google
     * Accepts the Google ID token from the frontend OIDC/PKCE flow.
     * Returns WorkforceHub internal access + refresh tokens.
     */
    @PostMapping("/google")
    @Operation(summary = "Exchange Google ID token for WorkforceHub JWT pair")
    public ResponseEntity<AuthResponse> googleLogin(
            @Valid @RequestBody GoogleTokenRequest request,
            HttpServletRequest httpRequest) {

        rateLimitService.checkRateLimit(getClientIp(httpRequest));
        log.info("Google login attempt from IP={}", getClientIp(httpRequest));

        AuthResponse response = authService.loginWithGoogle(request.idToken());
        log.info("Google login successful: userId={}", response.userId());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /auth/refresh
     * Single-use refresh token rotation — old token is revoked, new pair issued.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token — get new access + refresh token pair")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        rateLimitService.checkRateLimit(getClientIp(httpRequest));
        return ResponseEntity.ok(authService.refreshAccessToken(request.refreshToken()));
    }

    /**
     * POST /auth/logout
     * Revokes the provided refresh token for this device.
     * Access token expires naturally (< 1h).
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout — revoke current session's refresh token")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /**
     * POST /auth/logout-all
     * Revokes ALL refresh tokens for the user.
     * The X-User-Id header is injected by the gateway after JWT validation.
     */
    @PostMapping("/logout-all")
    @Operation(summary = "Logout all devices — revoke all refresh tokens")
    public ResponseEntity<Map<String, String>> logoutAll(
            @RequestHeader("X-User-Id") String userId) {
        authService.logoutAllDevices(userId);
        return ResponseEntity.ok(Map.of("message", "All sessions revoked"));
    }

    /**
     * GET /auth/me
     * Returns decoded claims from the current access token.
     * The gateway injects X-User-Id, X-Tenant-Id, X-User-Role headers — no DB hit
     * needed.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile from token claims")
    public ResponseEntity<Map<String, Object>> me(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-User-Email", required = false) String email) {
        return ResponseEntity.ok(Map.of(
                "id", userId,
                "tenantId", tenantId,
                "role", role,
                "email", email != null ? email : ""));
    }

    @GetMapping("/health")
    @Operation(summary = "Auth service health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "workforce-auth-service"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isBlank()) {
            return xForwarded.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
