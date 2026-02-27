package com.test.system.service.security;

import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.test.system.config.RateLimitConfig.*;

/**
 * Service for rate limiting critical endpoints.
 * Uses Bucket4j token bucket algorithm to prevent abuse.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final Map<String, Bucket> rateLimitBuckets;

    /**
     * Check if login attempt is allowed for this IP.
     * @param request HTTP request to extract IP from
     * @return true if allowed, false if rate limit exceeded
     */
    public boolean allowLogin(HttpServletRequest request) {
        String ip = getClientIp(request);
        String key = "login:" + ip;
        
        Bucket bucket = rateLimitBuckets.computeIfAbsent(key, k -> createLoginBucket());
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            log.warn("[RateLimit] Login blocked for IP: {}", ip);
        }
        
        return allowed;
    }

    /**
     * Check if registration is allowed for this IP.
     */
    public boolean allowRegister(HttpServletRequest request) {
        String ip = getClientIp(request);
        String key = "register:" + ip;
        
        Bucket bucket = rateLimitBuckets.computeIfAbsent(key, k -> createRegisterBucket());
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            log.warn("[RateLimit] Registration blocked for IP: {}", ip);
        }
        
        return allowed;
    }

    /**
     * Check if password reset request is allowed for this email.
     */
    public boolean allowPasswordReset(String email) {
        String key = "password-reset:" + email.toLowerCase();
        
        Bucket bucket = rateLimitBuckets.computeIfAbsent(key, k -> createPasswordResetBucket());
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            log.warn("[RateLimit] Password reset blocked for email: {}", email);
        }
        
        return allowed;
    }

    /**
     * Check if invite sending is allowed for this user.
     */
    public boolean allowInvite(Long userId) {
        String key = "invite:" + userId;
        
        Bucket bucket = rateLimitBuckets.computeIfAbsent(key, k -> createInviteBucket());
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            log.warn("[RateLimit] Invite sending blocked for user: {}", userId);
        }
        
        return allowed;
    }

    /**
     * Extract client IP from request, considering proxy headers.
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // If multiple IPs in X-Forwarded-For, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}

