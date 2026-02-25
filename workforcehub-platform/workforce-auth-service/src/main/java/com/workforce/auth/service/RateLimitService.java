package com.workforce.auth.service;

import com.workforce.auth.exception.RateLimitExceededException;
import io.github.bucket4j.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token-bucket rate limiter for the /auth/google endpoint.
 * Uses an in-process ConcurrentHashMap of Bucket4j buckets (suitable for single-node dev).
 *
 * For multi-node production: replace with Bucket4j-Redis distributed buckets.
 * See: https://bucket4j.com/8.x/toc.html#bucket4j-redis
 */
@Slf4j
@Service
public class RateLimitService {

    // 10 requests per minute per IP — configurable in application.yml
    private static final int CAPACITY        = 10;
    private static final int REFILL_PER_MIN  = 10;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void checkRateLimit(String clientIp) {
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::newBucket);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            log.warn("Rate limit exceeded for IP={}, retry after {}s", clientIp, waitSeconds);
            throw new RateLimitExceededException(
                    "Too many authentication attempts. Please try again in " + waitSeconds + " seconds.");
        }

        log.debug("Rate limit check passed for IP={}, remaining={}", clientIp, probe.getRemainingTokens());
    }

    private Bucket newBucket(String ip) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(CAPACITY)
                .refillGreedy(REFILL_PER_MIN, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
