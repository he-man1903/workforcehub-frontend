package com.workforce.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Stores OAuth2 state parameter in Redis for CSRF protection.
 * State is consumed once when the callback is handled.
 */
@Component
@RequiredArgsConstructor
public class OAuth2StateStore {

    private static final String PREFIX = "oauth2:state:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;

    public void save(String state) {
        redis.opsForValue().set(PREFIX + state, "1", TTL);
    }

    /** Returns true if the state existed and was deleted (one-time use). */
    public boolean consume(String state) {
        String key = PREFIX + state;
        Boolean deleted = redis.delete(key);
        return Boolean.TRUE.equals(deleted);
    }
}

