package com.test.system.repository.group;

import com.test.system.model.group.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing groups (collaborative workspaces).
 *
 * <p>A group is a collaborative container that owns projects and has members with different roles.
 * Each user automatically gets a personal group upon registration, which cannot be deleted.
 *
 * <p><b>Group types:</b>
 * <ul>
 *   <li><b>Personal group</b> - Automatically created for each user, marked with {@code personal=true}.
 *       There should be exactly one personal group per user. Cannot be deleted.</li>
 *   <li><b>Team group</b> - Created manually by users for collaboration, marked with {@code personal=false}.
 *       Can be deleted by the owner.</li>
 * </ul>
 *
 * @see Group
 * @see com.test.system.model.group.GroupMembership
 */
public interface GroupRepository extends JpaRepository<Group, Long> {

    /**
     * Find the personal group for a user.
     *
     * <p>Each user has exactly one personal group, automatically created during registration.
     * Personal groups are used as the default workspace for individual users and cannot be deleted.
     *
     * <p><b>Usage:</b> This method is called during user registration and OAuth2 login to ensure
     * the user has a personal group. If not found, a new one is created.
     *
     * @param ownerId the user ID (owner of the personal group)
     * @return the personal group if found, empty otherwise
     * @see com.test.system.service.authorization.UserService#ensurePersonalGroup(com.test.system.model.user.User)
     */
    @Query("SELECT g FROM Group g WHERE g.owner.id = :ownerId AND g.personal = true")
    Optional<Group> findPersonalGroup(@Param("ownerId") Long ownerId);

    /**
     * Find all groups owned by a user.
     *
     * @param ownerId the user ID (owner of the groups)
     * @return list of groups owned by the user
     */
    @Query("SELECT g FROM Group g WHERE g.owner.id = :ownerId")
    List<Group> findAllByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Delete all groups owned by a user using native SQL.
     * This bypasses Hibernate and lets PostgreSQL CASCADE handle related deletions.
     *
     * @param ownerId the user ID (owner of the groups)
     * @return number of groups deleted
     */
    @Modifying
    @Query(value = "DELETE FROM groups WHERE owner_id = :ownerId", nativeQuery = true)
    int deleteAllByOwnerId(@Param("ownerId") Long ownerId);

}
