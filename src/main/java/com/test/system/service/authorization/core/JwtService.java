package com.test.system.service.authorization.core;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Service for generating and validating JWT tokens for user authentication.
 * Uses HMAC-SHA256 signature algorithm for token signing.
 */
@Service
@Slf4j
public class JwtService {

    private static final String LOG_PREFIX = "[JWT]";

    private final Key signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expirationMs:3600000}") long expirationMs
    ) {
        this.signingKey = initSigningKey(secret);
        this.expirationMs = expirationMs;

        log.info("{} service initialized: expirationMs={} (~{} min)", LOG_PREFIX, expirationMs, expirationMs / 60000);
    }

    /* ===================== issue ===================== */

    /**
     * Generates a new JWT token with the given subject and claims.
     */
    public String generateToken(String subject, Map<String, Object> claims) {
        String normalizedSubject = normalizeSubject(subject);
        Date now = new Date();

        String token = Jwts.builder()
                .setSubject(normalizedSubject)
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        log.trace("{} token generated for subject='{}'", LOG_PREFIX, normalizedSubject);
        return token;
    }

    /* ===================== parse ===================== */

    /**
     * Extracts and returns the subject (email) from a JWT token.
     */
    public String extractSubject(String token) {
        String subject = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();

        log.trace("{} subject extracted='{}'", LOG_PREFIX, subject);
        return subject;
    }

    /* ===================== helpers ===================== */

    private Key initSigningKey(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            log.warn("{} weak secret key ({} bytes). HS256 recommends at least 32 bytes.", LOG_PREFIX, bytes.length);
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    private String normalizeSubject(String subject) {
        if (subject == null) {
            return "";
        }
        return subject.trim().toLowerCase(Locale.ROOT);
    }
}
