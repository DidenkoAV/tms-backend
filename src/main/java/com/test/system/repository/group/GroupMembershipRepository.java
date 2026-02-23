package com.test.system.repository.group;

import com.test.system.enums.groups.MembershipStatus;
import com.test.system.model.group.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing group memberships.
 *
 * <p>A membership represents a user's relationship with a group, including their role
 * (OWNER, MAINTAINER, MEMBER) and status (ACTIVE, PENDING, REMOVED).
 *
 * <p><b>Membership statuses:</b>
 * <ul>
 *   <li>{@link MembershipStatus#ACTIVE} - User is an active member of the group</li>
 *   <li>{@link MembershipStatus#PENDING} - User has been invited but hasn't accepted yet</li>
 *   <li>{@link MembershipStatus#REMOVED} - User has left or been removed from the group</li>
 * </ul>
 *
 * @see GroupMembership
 * @see MembershipStatus
 */
public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {

    /**
     * Find a membership for a specific user in a specific group (any status).
     * Used to check if a user is already a member or has a pending invitation.
     *
     * @param groupId the group ID
     * @param userId  the user ID
     * @return the membership if found, empty otherwise
     */
    @Query("SELECT m FROM GroupMembership m WHERE m.group.id = :groupId AND m.user.id = :userId")
    Optional<GroupMembership> findMembership(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /**
     * Get all memberships for a user with a specific status.
     * Used to list all groups a user is actively part of, or pending invitations.
     *
     * @param userId the user ID
     * @param status the membership status (ACTIVE, PENDING, or REMOVED)
     * @return list of memberships
     */
    @Query("SELECT m FROM GroupMembership m WHERE m.user.id = :userId AND m.status = :status")
    List<GroupMembership> findUserMembershipsByStatus(@Param("userId") Long userId, @Param("status") MembershipStatus status);

    /**
     * Get all memberships in a group with a specific status.
     * Used to list active members, pending invitations, or removed members.
     *
     * @param groupId the group ID
     * @param status  the membership status (ACTIVE, PENDING, or REMOVED)
     * @return list of memberships
     */
    @Query("SELECT m FROM GroupMembership m WHERE m.group.id = :groupId AND m.status = :status")
    List<GroupMembership> findGroupMembershipsByStatus(@Param("groupId") Long groupId, @Param("status") MembershipStatus status);

    /**
     * Count memberships in a group with a specific status.
     * Used to check group size limits (e.g., max 3 active members).
     *
     * @param groupId the group ID
     * @param status  the membership status (typically ACTIVE)
     * @return number of memberships
     */
    @Query("SELECT COUNT(m) FROM GroupMembership m WHERE m.group.id = :groupId AND m.status = :status")
    int countMembershipsByStatus(@Param("groupId") Long groupId, @Param("status") MembershipStatus status);

    /**
     * Delete all memberships for a group (used when deleting a group).
     * This is a cascade operation that removes all member relationships.
     *
     * @param groupId the group ID
     */
    @Modifying
    @Query("DELETE FROM GroupMembership m WHERE m.group.id = :groupId")
    void deleteAllMemberships(@Param("groupId") Long groupId);

    /**
     * Check if a user has a membership in a group with a specific status.
     * Used for quick permission checks (e.g., is user an active member?).
     *
     * @param groupId the group ID
     * @param userId  the user ID
     * @param status  the membership status (typically ACTIVE)
     * @return true if membership exists with the given status, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM GroupMembership m WHERE m.group.id = :groupId AND m.user.id = :userId AND m.status = :status")
    boolean hasMembershipWithStatus(@Param("groupId") Long groupId, @Param("userId") Long userId, @Param("status") MembershipStatus status);
}
