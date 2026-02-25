package com.workforce.common.security;

import com.workforce.common.config.JwtProperties;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Base Spring Security configuration shared across all downstream services.
 *
 * Each service extends this and calls super.applyCommonSecurity(http, jwtProperties)
 * in their own @Bean SecurityFilterChain method.
 *
 * What it does:
 *  - Disables sessions (STATELESS)
 *  - Disables CSRF (REST API)
 *  - Installs JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter
 *  - Permits /actuator/health and /actuator/info without auth
 *  - All other paths require authentication
 */
public abstract class BaseSecurityConfig {

    protected HttpSecurity applyCommonSecurity(HttpSecurity http, JwtProperties jwtProperties)
            throws Exception {

        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtProperties);

        return http
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                );
    }
}
