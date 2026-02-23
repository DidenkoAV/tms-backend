package com.test.system.repository.auth;

import com.test.system.enums.auth.TokenType;
import com.test.system.model.auth.VerificationToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for managing verification tokens used in email verification,
 * password reset, email change, and group invitation flows.
 *
 * <p>Tokens are one-time use and have an expiration time. Once used, the {@code usedAt}
 * field is set, preventing reuse. Expired tokens are periodically cleaned up.
 *
 * <p><b>Token types:</b>
 * <ul>
 *   <li>{@link com.test.system.enums.auth.TokenType#EMAIL_VERIFY} - Email verification during registration</li>
 *   <li>{@link com.test.system.enums.auth.TokenType#PASSWORD_RESET} - Password reset flow</li>
 *   <li>{@link com.test.system.enums.auth.TokenType#EMAIL_CHANGE} - Email change confirmation</li>
 *   <li>{@link com.test.system.enums.auth.TokenType#GROUP_INVITE} - Group invitation acceptance</li>
 * </ul>
 *
 * @see VerificationToken
 * @see TokenType
 */
public interface UserVerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    /**
     * Find an active (unused and not expired) verification token by its hash and type.
     *
     * <p>A token is considered active if:
     * <ul>
     *   <li>It matches the provided hash and type</li>
     *   <li>It has not been used yet ({@code usedAt IS NULL})</li>
     *   <li>It has not expired ({@code expiresAt > now})</li>
     * </ul>
     *
     * <p>This method is used during token validation (e.g., email verification, password reset).
     *
     * @param tokenHash the SHA-256 hash of the raw token
     * @param type      the token type (EMAIL_VERIFY, PASSWORD_RESET, etc.)
     * @param now       the current timestamp to check expiration
     * @return the active token if found, empty otherwise
     */
    @Query("SELECT t FROM VerificationToken t WHERE t.tokenHash = :tokenHash AND t.type = :type AND t.usedAt IS NULL AND t.expiresAt > :now")
    Optional<VerificationToken> findActiveToken(
            @Param("tokenHash") String tokenHash,
            @Param("type") TokenType type,
            @Param("now") Instant now
    );

    /**
     * Delete all active (unused) tokens for a specific user and type.
     *
     * <p>This is used to invalidate previous tokens before issuing a new one.
     * For example, when sending a new password reset email, we delete any existing
     * unused password reset tokens for that user.
     *
     * <p><b>Usage examples:</b>
     * <ul>
     *   <li>Before sending a new email verification token</li>
     *   <li>Before sending a new password reset token</li>
     *   <li>Before sending a new email change confirmation</li>
     *   <li>Before sending a new group invitation</li>
     * </ul>
     *
     * @param userId the user ID
     * @param type   the token type to delete
     * @return the number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.user.id = :userId AND t.type = :type AND t.usedAt IS NULL")
    int deleteActiveTokensByUserAndType(@Param("userId") Long userId, @Param("type") TokenType type);

    /**
     * Delete all expired tokens (regardless of whether they were used).
     *
     * <p>This method is used for periodic cleanup to prevent the database from growing indefinitely.
     * It's typically called by a scheduled job (e.g., hourly or daily).
     *
     * <p>Tokens are considered expired if {@code expiresAt < cutoff}.
     *
     * @param cutoff the cutoff timestamp (typically {@code Instant.now()})
     * @return the number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.expiresAt < :cutoff")
    long deleteExpiredTokens(@Param("cutoff") Instant cutoff);
}

