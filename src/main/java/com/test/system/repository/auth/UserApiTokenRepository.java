package com.test.system.repository.auth;

import com.test.system.model.auth.ApiToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing user API tokens (Personal Access Tokens).
 * Handles token lookup, listing active tokens, and retrieving token history.
 */
public interface UserApiTokenRepository extends JpaRepository<ApiToken, UUID> {

    /**
     * Find an active (non-revoked) API token by its prefix.
     * Used during authentication to validate incoming API tokens.
     *
     * @param tokenPrefix the token prefix to search for
     * @return the active token if found, empty otherwise
     */
    @Query("SELECT t FROM ApiToken t WHERE t.tokenPrefix = :tokenPrefix AND t.revokedAt IS NULL")
    Optional<ApiToken> findActiveTokenByPrefix(@Param("tokenPrefix") String tokenPrefix);

    /**
     * Get all active (non-revoked) API tokens for a user, ordered by creation date (newest first).
     * Used to display the user's current active tokens in the UI.
     *
     * @param userId the user ID
     * @return list of active tokens, newest first
     */
    @Query("SELECT t FROM ApiToken t WHERE t.user.id = :userId AND t.revokedAt IS NULL ORDER BY t.createdAt DESC")
    List<ApiToken> findActiveTokensByUser(@Param("userId") Long userId);

    /**
     * Get all API tokens (both active and revoked) for a user, ordered by creation date (newest first).
     * Used to display the complete token history including revoked tokens.
     *
     * @param userId the user ID
     * @return list of all tokens, newest first
     */
    @Query("SELECT t FROM ApiToken t WHERE t.user.id = :userId ORDER BY t.createdAt DESC")
    List<ApiToken> findAllTokensByUser(@Param("userId") Long userId);
}

