package com.test.system.service.authorization.core;

import com.test.system.enums.auth.TokenType;
import com.test.system.model.auth.VerificationToken;
import com.test.system.model.user.User;
import com.test.system.repository.auth.UserVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Service for managing one-time email verification tokens.
 * Used for email verification, password reset, email change, and group invitations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTokenService {

    private static final String LOG_PREFIX = "[EmailToken]";

    private final UserVerificationTokenRepository repo;

    /* ===================== Static utils ===================== */

    /**
     * Generates a new random token string in format: uuid1.uuid2
     */
    public static String newRawToken() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }

    /**
     * Computes SHA-256 hash of a string and returns it as hex string.
     */
    public static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /* ===================== Creation ===================== */

    /**
     * Creates a new email verification token and stores its SHA-256 hash in the database.
     */
    @Transactional
    public VerificationToken createToken(User user, TokenType type, Duration ttl, String rawToken) {
        Instant now = Instant.now();

        VerificationToken token = VerificationToken.builder()
                .user(user)
                .type(type)
                .tokenHash(sha256Hex(rawToken))
                .expiresAt(now.plus(ttl))
                .build();

        VerificationToken saved = repo.save(token);

        log.debug("{} created: tokenId={}, userId={}, type={}, expiresAt={}", LOG_PREFIX, saved.getId(), user.getId(), type, saved.getExpiresAt());

        return saved;
    }

    /* ===================== Validation ===================== */

    /**
     * Validates and consumes an active token (one-time use only).
     */
    @Transactional
    public VerificationToken validateAndConsumeToken(String rawToken, TokenType type) {
        Instant now = Instant.now();
        String hash = sha256Hex(rawToken);

        VerificationToken token = repo
                .findActiveToken(hash, type, now)
                .orElseThrow(() -> new IllegalArgumentException("Token invalid or expired"));

        token.setUsedAt(now);

        log.debug("{} consumed: tokenId={}, userId={}, type={}, usedAt={}", LOG_PREFIX, token.getId(), token.getUser().getId(), type, token.getUsedAt());

        return token;
    }

    /* ===================== Cleanup ===================== */

    /**
     * Deletes all active tokens for a specific user and token type.
     */
    @Transactional
    public int deleteActiveTokens(Long userId, TokenType type) {
        int deleted = repo.deleteActiveTokensByUserAndType(userId, type);

        if (deleted > 0) {
            log.debug("{} deleted {} active: userId={}, type={}", LOG_PREFIX, deleted, userId, type);
        }

        return deleted;
    }

}
