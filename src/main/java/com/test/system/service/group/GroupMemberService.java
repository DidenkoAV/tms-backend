package com.test.system.service.group;

import com.test.system.component.group.GroupAccessControl;
import com.test.system.enums.groups.GroupError;
import com.test.system.enums.groups.GroupRole;
import com.test.system.enums.groups.MembershipStatus;
import com.test.system.model.group.Group;
import com.test.system.model.group.GroupMembership;
import com.test.system.model.user.User;
import com.test.system.repository.group.GroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static com.test.system.component.group.GroupAccessControl.normEmail;

/**
 * Service for managing group members.
 * Handles member removal and role changes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroupMemberService {

    private static final String LOG_PREFIX = "[GroupMember]";

    private final GroupMembershipRepository memberships;
    private final GroupAccessControl accessControl;

    /**
     * Removes a member from the group by membership ID.
     * Only OWNER can remove members. Cannot remove OWNER or PENDING members.
     *
     * @param groupId group ID
     * @param membershipId membership ID to remove
     * @param requesterEmailRaw email of the requester (must be OWNER)
     * @throws ResponseStatusException if validation fails
     */
    @Transactional
    public void removeMember(Long groupId, Long membershipId, String requesterEmailRaw) {
        String requesterEmail = normEmail(requesterEmailRaw);
        log.warn("{} remove member: groupId={}, membershipId={}, by={}", LOG_PREFIX, groupId, membershipId, requesterEmail);

        Group g = accessControl.requireGroup(groupId);
        User requester = accessControl.requireUser(requesterEmail);
        accessControl.requireOwnerActiveMember(groupId, requester.getId());

        GroupMembership m = memberships.findById(membershipId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        GroupError.MEMBERSHIP_NOT_FOUND.name()
                ));

        if (!m.getGroup().getId().equals(g.getId())) {
            log.error("{} membership group mismatch: membershipId={}, expectedGroupId={}", LOG_PREFIX, membershipId, groupId);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    GroupError.MEMBERSHIP_GROUP_MISMATCH.name()
            );
        }

        if (m.getUser().getId().equals(g.getOwner().getId())) {
            log.warn("{} attempt to remove owner: groupId={}, ownerId={}", LOG_PREFIX, groupId, g.getOwner().getId());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    GroupError.CANNOT_REMOVE_OWNER.name()
            );
        }

        if (m.getStatus() == MembershipStatus.PENDING) {
            log.warn("{} wrong endpoint to remove PENDING member: membershipId={}", LOG_PREFIX, membershipId);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    GroupError.USE_INVITE_CANCEL_ENDPOINT.name()
            );
        }

        m.setStatus(MembershipStatus.REMOVED);
        memberships.save(m);

        log.info("{} member removed: groupId={}, membershipId={}", LOG_PREFIX, groupId, membershipId);
    }

    /**
     * Changes the role of a group member.
     * Only OWNER can change roles. Cannot change OWNER role or promote to OWNER.
     *
     * @param groupId group ID
     * @param membershipId membership ID to update
     * @param roleStr new role as string (MEMBER or MAINTAINER)
     * @param requesterEmailRaw email of the requester (must be OWNER)
     * @throws ResponseStatusException if validation fails
     */
    @Transactional
    public void changeMemberRole(Long groupId, Long membershipId, String roleStr, String requesterEmailRaw) {
        String requesterEmail = normEmail(requesterEmailRaw);
        log.info("{} change role: groupId={}, membershipId={}, by={}, newRole={}",
                LOG_PREFIX, groupId, membershipId, requesterEmail, roleStr);

        Group g = accessControl.requireGroup(groupId);
        User requester = accessControl.requireUser(requesterEmail);
        accessControl.requireOwnerActiveMember(groupId, requester.getId());

        GroupMembership m = memberships.findById(membershipId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        GroupError.MEMBERSHIP_NOT_FOUND.name()
                ));

        if (!m.getGroup().getId().equals(g.getId())) {
            log.error("{} membership group mismatch on role change: membershipId={}, groupId={}",
                    LOG_PREFIX, membershipId, groupId);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    GroupError.MEMBERSHIP_GROUP_MISMATCH.name()
            );
        }

        if (m.getUser().getId().equals(g.getOwner().getId())) {
            log.warn("{} attempt to change owner role: groupId={}", LOG_PREFIX, groupId);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    GroupError.OWNER_ROLE_FIXED.name()
            );
        }

        if (m.getStatus() != MembershipStatus.ACTIVE) {
            log.warn("{} role change for non-active member: membershipId={}, status={}",
                    LOG_PREFIX, membershipId, m.getStatus());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    GroupError.MEMBER_NOT_ACTIVE.name()
            );
        }

        GroupRole role = GroupRole.valueOf(roleStr.toUpperCase());
        if (role == GroupRole.OWNER) {
            log.warn("{} attempt to promote to OWNER: membershipId={}", LOG_PREFIX, membershipId);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    GroupError.CANNOT_PROMOTE_TO_OWNER.name()
            );
        }

        m.setRole(role);
        memberships.save(m);

        log.info("{} role updated: groupId={}, membershipId={}, role={}", LOG_PREFIX, groupId, membershipId, role);
    }
}

