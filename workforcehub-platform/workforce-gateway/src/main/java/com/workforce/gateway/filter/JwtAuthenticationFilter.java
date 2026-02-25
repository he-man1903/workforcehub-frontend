package com.workforce.gateway.filter;

import com.workforce.gateway.config.SecurityProperties;
import com.workforce.gateway.security.GatewayHeaders;
import com.workforce.gateway.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityProperties securityProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter() {
        super(Config.class);
        this.jwtTokenProvider = null;
        this.securityProperties = null;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();

            // Skip public paths
            boolean isPublic = securityProperties.getPublicPaths().stream()
                    .anyMatch(p -> pathMatcher.match(p, path));
            if (isPublic) {
                return chain.filter(exchange);
            }

            // Extract Bearer token
            String authHeader = request.getHeaders().getFirst(GatewayHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith(GatewayHeaders.BEARER_PREFIX)) {
                log.warn("Missing or malformed Authorization header for path: {}", path);
                return unauthorized(exchange, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(GatewayHeaders.BEARER_PREFIX.length());
            Optional<Claims> claimsOpt = jwtTokenProvider.validateAndParseClaims(token);

            if (claimsOpt.isEmpty()) {
                log.warn("JWT validation failed for path: {}", path);
                return unauthorized(exchange, "Invalid or expired JWT token");
            }

            Claims claims = claimsOpt.get();
            String tenantId = jwtTokenProvider.extractTenantId(claims);
            String userId = jwtTokenProvider.extractUserId(claims);
            String role = jwtTokenProvider.extractRole(claims);
            String email = jwtTokenProvider.extractEmail(claims);

            if (tenantId == null || tenantId.isBlank()) {
                log.warn("JWT missing tenantId claim for userId={}", userId);
                return unauthorized(exchange, "JWT missing required tenantId claim");
            }

            log.debug("Authenticated request: userId={}, tenantId={}, path={}", userId, tenantId, path);

            // Mutate request to add downstream internal headers
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(GatewayHeaders.X_TENANT_ID, tenantId)
                    .header(GatewayHeaders.X_USER_ID, userId)
                    .header(GatewayHeaders.X_USER_ROLE, role != null ? role : "")
                    .header(GatewayHeaders.X_USER_EMAIL, email != null ? email : "")
                    // Strip original Authorization header from downstream (principle of least
                    // privilege)
                    .headers(h -> h.remove(GatewayHeaders.AUTHORIZATION))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"status":401,"error":"Unauthorized","message":"%s"}
                """.formatted(message);
        var buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // Extensible config — e.g. required roles per route
    }
}
