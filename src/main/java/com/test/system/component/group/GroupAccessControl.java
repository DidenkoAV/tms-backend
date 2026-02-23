package com.test.system.component.group;

import com.test.system.enums.groups.GroupError;
import com.test.system.enums.groups.GroupRole;
import com.test.system.enums.groups.MembershipStatus;
import com.test.system.model.group.Group;
import com.test.system.model.group.GroupMembership;
import com.test.system.model.user.User;
import com.test.system.repository.group.GroupMembershipRepository;
import com.test.system.repository.group.GroupRepository;
import com.test.system.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * Helper component for group access control and validation.
 * Provides common validation and permission checking logic for group operations.
 */
@Component
@RequiredArgsConstructor
public class GroupAccessControl {

    private final UserRepository users;
    private final GroupRepository groups;
    private final GroupMembershipRepository memberships;

    /**
     * Finds user by email or throws exception.
     *
     * @param emailRaw raw email address (will be normalized)
     * @return User entity
     * @throws ResponseStatusException if user not found
     */
    public User requireUser(String emailRaw) {
        String email = normEmail(emailRaw);
        return users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        GroupError.USER_NOT_FOUND.name()
                ));
    }

    /**
     * Finds group by ID or throws exception.
     *
     * @param groupId group ID
     * @return Group entity
     * @throws ResponseStatusException if group not found
     */
    public Group requireGroup(Long groupId) {
        return groups.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        GroupError.GROUP_NOT_FOUND.name()
                ));
    }

    /**
     * Requires user to be OWNER and ACTIVE member of the group.
     *
     * @param groupId group ID
     * @param userId user ID
     * @return GroupMembership entity
     * @throws ResponseStatusException if user is not OWNER or not ACTIVE
     */
    public GroupMembership requireOwnerActiveMember(Long groupId, Long userId) {
        GroupMembership m = memberships.findMembership(groupId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        GroupError.NOT_A_MEMBER.name()
                ));

        if (m.getStatus() != MembershipStatus.ACTIVE || m.getRole() != GroupRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, GroupError.OWNER_ONLY.name());
        }
        return m;
    }

    /**
     * Requires user to be ACTIVE member of the group.
     *
     * @param groupId group ID
     * @param userId user ID
     * @return GroupMembership entity
     * @throws ResponseStatusException if user is not ACTIVE member
     */
    public GroupMembership requireActiveMember(Long groupId, Long userId) {
        GroupMembership m = memberships.findMembership(groupId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        GroupError.NOT_A_MEMBER.name()
                ));

        if (m.getStatus() != MembershipStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    GroupError.MEMBERSHIP_NOT_ACTIVE.name()
            );
        }
        return m;
    }

    /**
     * Requires user to have at least the specified role.
     *
     * @param groupId group ID
     * @param userId user ID
     * @param minRole minimum required role
     * @throws ResponseStatusException if user doesn't have required role
     */
    public void requireAtLeast(Long groupId, Long userId, GroupRole minRole) {
        GroupMembership m = requireActiveMember(groupId, userId);
        if (roleRank(m.getRole()) < roleRank(minRole)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    minRole.name() + "_OR_HIGHER_ONLY"
            );
        }
    }

    /**
     * Normalizes email address to lowercase.
     *
     * @param email raw email address
     * @return normalized email (lowercase, trimmed)
     */
    public static String normEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns safe name or fallback to email.
     *
     * @param name user's full name
     * @param fallbackEmail email to use if name is empty
     * @return name or email
     */
    public static String safeName(String name, String fallbackEmail) {
        String n = name == null ? "" : name.trim();
        return n.isEmpty() ? fallbackEmail : n;
    }

    /**
     * Returns role rank for comparison.
     * Higher rank = more permissions.
     */
    private static int roleRank(GroupRole r) {
        return switch (r) {
            case OWNER -> 3;
            case MAINTAINER -> 2;
            case MEMBER -> 1;
        };
    }
}

