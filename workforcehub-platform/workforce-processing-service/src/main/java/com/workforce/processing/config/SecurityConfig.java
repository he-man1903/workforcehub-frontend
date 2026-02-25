package com.workforce.processing.config;

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
 * Stateless JWT security for the processing service.
 * Processing service is primarily Kafka-driven (no HTTP endpoints),
 * but actuator endpoints still need security configuration.
 * See SecurityConfig in upload-service for full architecture notes.
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
