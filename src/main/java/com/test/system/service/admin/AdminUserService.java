package com.test.system.service.admin;

import com.test.system.dto.admin.UserListResponse;
import com.test.system.enums.auth.RoleName;
import com.test.system.enums.auth.TokenType;
import com.test.system.enums.groups.MembershipStatus;
import com.test.system.model.group.Group;
import com.test.system.model.group.GroupMembership;
import com.test.system.model.user.User;
import com.test.system.model.user.UserRole;
import com.test.system.repository.auth.UserRoleRepository;
import com.test.system.repository.group.GroupMembershipRepository;
import com.test.system.repository.group.GroupRepository;
import com.test.system.repository.user.UserRepository;
import com.test.system.service.authorization.core.EmailTokenService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for admin user management operations.
 * Provides functionality for administrators to manage users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private static final String LOG_PREFIX = "[AdminUserService]";

    private final UserRepository userRepository;
    private final UserRoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final EmailTokenService tokenService;
    private final EntityManager entityManager;

    /**
     * Lists all users in the system.
     * Only accessible by administrators.
     *
     * @param requesterEmail email of the admin making the request
     * @return list of all users with their details
     * @throws ResponseStatusException if requester is not an admin
     */
    @Transactional(readOnly = true)
    public List<UserListResponse> listAllUsers(String requesterEmail) {
        log.info("{} listAllUsers: requester={}", LOG_PREFIX, requesterEmail);

        // Verify requester is admin
        User requester = userRepository.findWithAllByEmail(requesterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!isAdmin(requester)) {
            log.warn("{} listAllUsers: unauthorized access attempt by {}", LOG_PREFIX, requesterEmail);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }

        // Fetch all users with their relationships
        List<User> users = userRepository.findAll();

        log.info("{} listAllUsers: found {} users", LOG_PREFIX, users.size());

        return users.stream()
                .map(this::toUserListResponse)
                .toList();
    }

    /**
     * Enables a user account.
     * Only accessible by administrators.
     *
     * @param userId user ID to enable
     * @param requesterEmail email of the admin making the request
     * @throws ResponseStatusException if requester is not an admin or user not found
     */
    @Transactional
    public void enableUser(Long userId, String requesterEmail) {
        log.info("{} enableUser: userId={}, requester={}", LOG_PREFIX, userId, requesterEmail);

        verifyAdmin(requesterEmail);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.isEnabled()) {
            log.warn("{} enableUser: user already enabled: userId={}", LOG_PREFIX, userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already enabled");
        }

        user.setEnabled(true);
        userRepository.save(user);

        log.info("{} enableUser: success userId={}, email={}", LOG_PREFIX, userId, user.getEmail());
    }

    /**
     * Disables a user account.
     * Only accessible by administrators.
     * Cannot disable admin users or yourself.
     *
     * @param userId user ID to disable
     * @param requesterEmail email of the admin making the request
     * @throws ResponseStatusException if requester is not an admin, user not found, or trying to disable admin/self
     */
    @Transactional
    public void disableUser(Long userId, String requesterEmail) {
        log.info("{} disableUser: userId={}, requester={}", LOG_PREFIX, userId, requesterEmail);

        User requester = verifyAdmin(requesterEmail);

        User user = userRepository.findWithAllById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Cannot disable yourself
        if (user.getId().equals(requester.getId())) {
            log.warn("{} disableUser: attempt to disable self: userId={}", LOG_PREFIX, userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot disable your own account");
        }

        // Cannot disable admin users
        if (isAdmin(user)) {
            log.warn("{} disableUser: attempt to disable admin: userId={}", LOG_PREFIX, userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot disable admin users");
        }

        if (!user.isEnabled()) {
            log.warn("{} disableUser: user already disabled: userId={}", LOG_PREFIX, userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already disabled");
        }

        user.setEnabled(false);
        userRepository.save(user);

        log.info("{} disableUser: success userId={}, email={}", LOG_PREFIX, userId, user.getEmail());
    }

    /**
     * Deletes a user permanently.
     * Only accessible by administrators.
     * Cannot delete yourself.
     * Deletes all user data including groups, memberships, tokens, etc. (CASCADE).
     *
     * @param userId user ID to delete
     * @param requesterEmail email of the admin making the request
     * @throws ResponseStatusException if requester is not an admin, user not found, or trying to delete yourself
     */
    @Transactional
    public void deleteUser(Long userId, String requesterEmail) {
        log.warn("{} deleteUser: userId={}, requester={}", LOG_PREFIX, userId, requesterEmail);

        User requester = verifyAdmin(requesterEmail);

        User user = userRepository.findWithAllById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Cannot delete yourself
        if (user.getId().equals(requester.getId())) {
            log.warn("{} deleteUser: attempt to delete self: userId={}", LOG_PREFIX, userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete your own account");
        }

        String deletedEmail = user.getEmail();

        // Step 1: Delete all pending invitation tokens for groups owned by this user
        List<Group> ownedGroups = groupRepository.findAllByOwnerId(userId);
        for (Group group : ownedGroups) {
            List<GroupMembership> pendingMemberships = membershipRepository.findGroupMembershipsByStatus(
                    group.getId(), MembershipStatus.PENDING);
            for (GroupMembership membership : pendingMemberships) {
                tokenService.deleteActiveTokens(membership.getUser().getId(), TokenType.GROUP_INVITE);
            }
        }

        // Step 2: Delete all groups owned by the user using native SQL (bypasses Hibernate, lets PostgreSQL CASCADE work)
        int deletedGroups = groupRepository.deleteAllByOwnerId(userId);
        log.info("{} deleteUser: deleted {} groups for userId={}", LOG_PREFIX, deletedGroups, userId);

        // Step 3: Flush and clear Hibernate session to avoid stale references
        entityManager.flush();
        entityManager.clear();

        // Step 4: Re-fetch the user (detached from deleted groups) and delete
        User userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found after group deletion"));
        userRepository.delete(userToDelete);

        log.warn("{} deleteUser: success userId={}, email={}, deletedGroups={}",
                LOG_PREFIX, userId, deletedEmail, deletedGroups);
    }

    /**
     * Updates user roles.
     * Only accessible by administrators.
     * Cannot modify admin users or yourself.
     * At least ROLE_USER must be present.
     *
     * @param userId user ID to update
     * @param roleNames list of role names to assign
     * @param requesterEmail email of the admin making the request
     * @throws ResponseStatusException if validation fails
     */
    @Transactional
    public void updateUserRoles(Long userId, List<String> roleNames, String requesterEmail) {
        log.info("{} updateUserRoles: userId={}, roles={}, requester={}", LOG_PREFIX, userId, roleNames, requesterEmail);

        User requester = verifyAdmin(requesterEmail);

        User user = userRepository.findWithAllById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Cannot modify yourself
        if (user.getId().equals(requester.getId())) {
            log.warn("{} updateUserRoles: attempt to modify self: userId={}", LOG_PREFIX, userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot modify your own roles");
        }

        // Cannot modify admin users
        if (isAdmin(user)) {
            log.warn("{} updateUserRoles: attempt to modify admin: userId={}", LOG_PREFIX, userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot modify admin user roles");
        }

        // Validate and convert role names
        Set<UserRole> newRoles = new HashSet<>();
        for (String roleName : roleNames) {
            try {
                RoleName roleEnum = RoleName.valueOf(roleName);
                UserRole role = roleRepository.findByName(roleEnum)
                        .orElseThrow(() -> new IllegalStateException("Role not found: " + roleName));
                newRoles.add(role);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role name: " + roleName);
            }
        }

        // Ensure at least ROLE_USER is present
        boolean hasUserRole = newRoles.stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_USER);
        if (!hasUserRole) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ROLE_USER is required");
        }

        // Determine primary role (ROLE_ADMIN takes precedence)
        RoleName primaryRole = newRoles.stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN)
                ? RoleName.ROLE_ADMIN
                : RoleName.ROLE_USER;

        user.getRoles().clear();
        user.getRoles().addAll(newRoles);
        user.setRole(primaryRole);
        userRepository.save(user);

        log.info("{} updateUserRoles: success userId={}, newRoles={}, primaryRole={}",
                LOG_PREFIX, userId, roleNames, primaryRole);
    }

    /* ===================== Helper Methods ===================== */

    /**
     * Verifies that requester is an admin.
     *
     * @param requesterEmail email of the requester
     * @return User entity of the requester
     * @throws ResponseStatusException if requester is not an admin
     */
    private User verifyAdmin(String requesterEmail) {
        User requester = userRepository.findWithAllByEmail(requesterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!isAdmin(requester)) {
            log.warn("{} unauthorized access attempt by {}", LOG_PREFIX, requesterEmail);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }

        return requester;
    }

    /**
     * Checks if user has ROLE_ADMIN.
     *
     * @param user user to check
     * @return true if user is admin, false otherwise
     */
    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
    }

    /**
     * Converts User entity to UserListResponse DTO.
     *
     * @param user user entity
     * @return user list response DTO
     */
    private UserListResponse toUserListResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .sorted()
                .toList();

        int groupCount = (int) user.getMemberships().stream()
                .filter(m -> m.getStatus().isActive())
                .count();

        return new UserListResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.isEnabled(),
                user.getRole().name(),
                roles,
                groupCount,
                user.getCreatedAt()
        );
    }
}

