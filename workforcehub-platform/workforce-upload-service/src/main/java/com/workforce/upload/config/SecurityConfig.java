package com.workforce.upload.config;

import com.workforce.common.config.JwtProperties;
import com.workforce.common.security.BaseSecurityConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless JWT security for the upload service.
 *
 * The service sits behind the API gateway, which:
 *   - Validates JWT signature and expiry
 *   - Injects X-Tenant-Id, X-User-Id, X-User-Role headers
 *   - Strips the raw Authorization header
 *
 * The shared JwtAuthenticationFilter (from workforce-common) provides
 * defence-in-depth by accepting EITHER the injected gateway headers OR
 * a raw Bearer token (for local dev / direct integration testing).
 *
 * No sessions, no redirects — stateless throughout.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig extends BaseSecurityConfig {

    private final JwtProperties jwtProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return applyCommonSecurity(http, jwtProperties).build();
    }
}
