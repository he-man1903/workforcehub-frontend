package com.workforce.common.security;

import com.workforce.common.config.JwtProperties;
import com.workforce.common.tenant.TenantContext;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Shared stateless JWT validation filter for all downstream services.
 *
 * In production, services receive requests from the gateway which has ALREADY
 * validated the JWT and stripped the Authorization header, injecting:
 *   X-User-Id    — internal user UUID
 *   X-Tenant-Id  — tenant scoping claim
 *   X-User-Role  — role (USER / ADMIN)
 *   X-User-Email — email claim
 *
 * This filter supports BOTH modes:
 *   1. Gateway mode — trust injected headers, skip JWT re-validation (fast path)
 *   2. Direct mode  — validate Bearer token from Authorization header (fallback / dev)
 *
 * The gateway mode is used in production (services are not publicly accessible).
 * The direct mode is retained for local development and integration testing.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX    = "Bearer ";
    private static final String HEADER_USER_ID   = "X-User-Id";
    private static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";
    private static final String HEADER_USER_EMAIL = "X-User-Email";
    private static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    private final JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        try {
            // ── Fast path: gateway has already validated the JWT ──────────────────
            String userId = request.getHeader(HEADER_USER_ID);
            String tenantId = request.getHeader(HEADER_TENANT_ID);
            String role = request.getHeader(HEADER_USER_ROLE);

            if (userId != null && tenantId != null) {
                setSecurityContext(userId, tenantId, role != null ? role : "USER");
                populateMdc(userId, tenantId, request.getHeader(HEADER_CORRELATION_ID));
                chain.doFilter(request, response);
                return;
            }

            // ── Fallback: validate Bearer token directly (dev / internal calls) ───
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                String token = authHeader.substring(BEARER_PREFIX.length());
                Claims claims = validateToken(token);
                if (claims != null) {
                    String sub      = claims.getSubject();
                    String tId      = claims.get("tenantId", String.class);
                    String r        = claims.get("role", String.class);
                    setSecurityContext(sub, tId != null ? tId : sub, r != null ? r : "USER");
                    populateMdc(sub, tId, request.getHeader(HEADER_CORRELATION_ID));
                }
            }

            chain.doFilter(request, response);

        } finally {
            TenantContext.clear();
            MDC.remove("userId");
            MDC.remove("tenantId");
            MDC.remove("correlationId");
        }
    }

    private void setSecurityContext(String userId, String tenantId, String role) {
        TenantContext.setTenantId(tenantId);
        var auth = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void populateMdc(String userId, String tenantId, String correlationId) {
        if (userId != null)        MDC.put("userId", userId);
        if (tenantId != null)      MDC.put("tenantId", tenantId);
        if (correlationId != null) MDC.put("correlationId", correlationId);
    }

    private Claims validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(jwtProperties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (claims.getExpiration().before(new Date())) {
                log.debug("JWT expired");
                return null;
            }
            return claims;
        } catch (JwtException e) {
            log.warn("JWT validation failed in downstream service: {}", e.getMessage());
            return null;
        }
    }
}
