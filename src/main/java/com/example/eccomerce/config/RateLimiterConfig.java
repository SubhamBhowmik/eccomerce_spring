package com.example.eccomerce.config;


import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableCaching
public class RateLimiterConfig {

    // Store bucket per user
    private final ConcurrentHashMap<String, Bucket> buckets
            = new ConcurrentHashMap<>();

    // Order placement — max 5 orders per minute per user
    public Bucket resolveOrderBucket(String userId) {
        return buckets.computeIfAbsent(userId + ":order", k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(
                                5, Refill.intervally(5, Duration.ofMinutes(1))
                        ))
                        .build()
        );
    }

    // Cart operations — max 30 per minute per user
    public Bucket resolveCartBucket(String userId) {
        return buckets.computeIfAbsent(userId + ":cart", k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(
                                30, Refill.intervally(30, Duration.ofMinutes(1))
                        ))
                        .build()
        );
    }

    // Login attempts — max 5 per minute per IP
    public Bucket resolveLoginBucket(String ip) {
        return buckets.computeIfAbsent(ip + ":login", k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(
                                5, Refill.intervally(5, Duration.ofMinutes(1))
                        ))
                        .build()
        );
    }

    // Register attempts — max 3 per hour per IP
    public Bucket resolveRegisterBucket(String ip) {
        return buckets.computeIfAbsent(ip + ":register", k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(
                                3, Refill.intervally(3, Duration.ofHours(1))
                        ))
                        .build()
        );
    }

    // General API — max 100 per minute per user
    public Bucket resolveApiBucket(String userId) {
        return buckets.computeIfAbsent(userId + ":api", k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(
                                100, Refill.intervally(100, Duration.ofMinutes(1))
                        ))
                        .build()
        );
    }
}