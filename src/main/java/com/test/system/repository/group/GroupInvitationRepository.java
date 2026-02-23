package com.test.system.repository.group;

import com.test.system.enums.groups.InvitationStatus;
import com.test.system.model.group.GroupInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing group invitations.
 * Handles invitation lookup, listing, and cleanup of expired invitations.
 */
public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {

    /**
     * Delete all expired invitations with the given status.
     * Used by scheduled cleanup job to remove expired PENDING invitations.
     *
     * @param status the invitation status (typically PENDING)
     * @param cutoff the cutoff timestamp (typically Instant.now())
     * @return the number of invitations deleted
     */
    @Modifying
    @Query("DELETE FROM GroupInvitation i WHERE i.status = :status AND i.expiresAt < :cutoff")
    int deleteExpiredInvitations(@Param("status") InvitationStatus status, @Param("cutoff") Instant cutoff);
}
