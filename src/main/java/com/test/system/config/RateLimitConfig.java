package com.test.system.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting configuration using Bucket4j.
 * Protects critical endpoints from abuse (brute force, spam, DoS).
 */
@Configuration
public class RateLimitConfig {

    /**
     * In-memory storage for rate limit buckets.
     * Key format: "endpoint:identifier" (e.g., "login:192.168.1.1" or "register:user@example.com")
     */
    @Bean
    public Map<String, Bucket> rateLimitBuckets() {
        return new ConcurrentHashMap<>();
    }

    /**
     * Login rate limit: 5 attempts per minute per IP
     */
    public static Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Register rate limit: 3 registrations per hour per IP
     */
    public static Bucket createRegisterBucket() {
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Password reset request rate limit: 3 requests per hour per email
     */
    public static Bucket createPasswordResetBucket() {
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Invite sending rate limit: 10 invites per hour per user
     */
    public static Bucket createInviteBucket() {
        Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofHours(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}

