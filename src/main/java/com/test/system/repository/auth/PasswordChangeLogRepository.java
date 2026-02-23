package com.test.system.repository.auth;

import com.test.system.model.auth.PasswordChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface PasswordChangeLogRepository extends JpaRepository<PasswordChangeLog, Long> {

    /**
     * Count password changes for a user since the given timestamp.
     * Used for rate limiting password changes (e.g., max 3 changes per 24 hours).
     *
     * @param userId the user ID
     * @param since  the timestamp to count from (exclusive)
     * @return number of password changes since the given time
     */
    @Query("SELECT COUNT(p) FROM PasswordChangeLog p WHERE p.user.id = :userId AND p.createdAt > :since")
    long countPasswordChangesSince(@Param("userId") Long userId, @Param("since") Instant since);
}

