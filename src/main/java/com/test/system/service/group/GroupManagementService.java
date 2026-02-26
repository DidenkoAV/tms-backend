package com.test.system.service.group;

import com.test.system.component.group.GroupAccessControl;
import com.test.system.component.group.GroupMapper;
import com.test.system.dto.group.info.GroupDetailsResponse;
import com.test.system.dto.group.info.GroupSummaryResponse;
import com.test.system.dto.group.member.GroupMemberResponse;
import com.test.system.enums.auth.TokenType;
import com.test.system.enums.groups.GroupError;
import com.test.system.enums.groups.GroupRole;
import com.test.system.enums.groups.GroupType;
import com.test.system.enums.groups.MembershipStatus;
import com.test.system.model.group.Group;
import com.test.system.model.group.GroupMembership;
import com.test.system.model.user.User;
import com.test.system.repository.group.GroupMembershipRepository;
import com.test.system.repository.group.GroupRepository;
import com.test.system.service.authorization.core.EmailTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.test.system.component.group.GroupAccessControl.normEmail;

/**
 * Service for managing groups.
 * Handles group operations: queries, updates, and lifecycle management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroupManagementService {

    private static final String LOG_PREFIX = "[Group]";

    private final GroupRepository groups;
    private final GroupMembershipRepository memberships;
    private final GroupAccessControl accessControl;
    private final EmailTokenService tokens;
    private final GroupMapper mapper;

    /* ===================== Query Operations ===================== */

    /**
     * Returns all groups where the user is an active member.
     *
     * @param emailRaw user's email address
     * @return list of group summaries
     */
    @Transactional(readOnly = true)
    public List<GroupSummaryResponse> myGroups(String emailRaw) {
        String email = normEmail(emailRaw);
        log.info("{} my groups: email={}", LOG_PREFIX, email);

        User me = accessControl.requireUser(email);
        var userMemberships = memberships.findUserMembershipsByStatus(me.getId(), MembershipStatus.ACTIVE);

        log.debug("{} my groups: userId={}, groups={}", LOG_PREFIX, me.getId(), userMemberships.size());

        return userMemberships.stream()
                .map(m -> {
                    Group g = m.getGroup();
                    int membersCount = memberships.countMembershipsByStatus(g.getId(), MembershipStatus.ACTIVE);
                    return mapper.toSummaryDto(g, membersCount);
                })
                .toList();
    }

    /**
     * Returns detailed information about a group including all members.
     *
     * @param groupId group ID
     * @param emailRaw user's email address
     * @return group details with member list
     */
    @Transactional(readOnly = true)
    public GroupDetailsResponse getGroupDetails(Long groupId, String emailRaw) {
        String email = normEmail(emailRaw);
        log.info("{} details: groupId={}, email={}", LOG_PREFIX, groupId, email);

        User me = accessControl.requireUser(email);
        Group g = accessControl.requireGroup(groupId);
        accessControl.requireActiveMember(g.getId(), me.getId());

        List<GroupMemberResponse> members = new ArrayList<>();
        memberships.findGroupMembershipsByStatus(g.getId(), MembershipStatus.ACTIVE)
                .forEach(m -> members.add(mapper.toMemberDto(m)));
        memberships.findGroupMembershipsByStatus(g.getId(), MembershipStatus.PENDING)
                .forEach(m -> members.add(mapper.toMemberDto(m)));

        log.debug("{} details: groupId={}, members={}", LOG_PREFIX, groupId, members.size());

        return mapper.toDetailsDto(g, members);
    }

    /* ===================== Group Operations ===================== */

    /**
     * Creates a new group with the current user as owner.
     * The group is created as non-personal and the creator is automatically added as OWNER.
     *
     * @param nameRaw group name
     * @param requesterEmailRaw creator's email
     * @return created group details
     */
    @Transactional
    public GroupDetailsResponse createGroup(String nameRaw, String requesterEmailRaw) {
        String requesterEmail = normEmail(requesterEmailRaw);
        log.info("{} create group: name='{}', creator={}", LOG_PREFIX, nameRaw, requesterEmail);

        User creator = accessControl.requireUser(requesterEmail);

        // Validate name
        String name = nameRaw == null ? "" : nameRaw.trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GroupError.NAME_REQUIRED.name());
        }
        if (name.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GroupError.NAME_TOO_SHORT.name());
        }
        if (name.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group name too long");
        }

        // Create group
        Group group = Group.builder()
                .name(name)
                .owner(creator)
                .groupType(GroupType.SHARED)
                .createdAt(Instant.now())
                .build();

        Group savedGroup = groups.save(group);

        // Create OWNER membership
        GroupMembership ownerMembership = GroupMembership.builder()
                .group(savedGroup)
                .user(creator)
                .role(GroupRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        memberships.save(ownerMembership);

        log.info("{} group created: groupId={}, name='{}', owner={}",
                LOG_PREFIX, savedGroup.getId(), savedGroup.getName(), creator.getEmail());

        // Return group details
        return getGroupDetails(savedGroup.getId(), requesterEmail);
    }

    /**
     * Updates the name of a group.
     * Only MAINTAINER or OWNER can rename groups.
     *
     * @param groupId group ID
     * @param newNameRaw new group name
     * @param requesterEmailRaw requester's email
     * @return updated Group entity
     */
    @Transactional
    public Group updateGroupName(Long groupId, String newNameRaw, String requesterEmailRaw) {
        String requesterEmail = normEmail(requesterEmailRaw);
        log.info("{} rename group: groupId={}, requester={}, newName='{}'",
                LOG_PREFIX, groupId, requesterEmail, newNameRaw);

        Group g = accessControl.requireGroup(groupId);
        User me = accessControl.requireUser(requesterEmail);

        accessControl.requireAtLeast(g.getId(), me.getId(), GroupRole.MAINTAINER);

        String next = newNameRaw == null ? "" : newNameRaw.trim();
        if (next.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GroupError.NAME_REQUIRED.name());
        }
        if (next.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GroupError.NAME_TOO_SHORT.name());
        }

        g.setName(next);
        g.setUpdatedAt(Instant.now());

        groups.save(g);
        log.info("{} group renamed: groupId={}, name='{}', by={}",
                LOG_PREFIX, g.getId(), g.getName(), me.getEmail());
        return g;
    }

    /**
     * Deletes a group permanently.
     * Only OWNER can delete groups. Personal groups cannot be deleted.
     *
     * @param groupId group ID
     * @param requesterEmailRaw requester's email (must be OWNER)
     */
    @Transactional
    public void deleteGroup(Long groupId, String requesterEmailRaw) {
        String requesterEmail = normEmail(requesterEmailRaw);
        log.warn("{} delete group: groupId={}, requester={}", LOG_PREFIX, groupId, requesterEmail);

        Group g = accessControl.requireGroup(groupId);
        User me = accessControl.requireUser(requesterEmail);

        if (!g.getOwner().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, GroupError.OWNER_ONLY.name());
        }
        if (g.isPersonal()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GroupError.CANNOT_DELETE_PERSONAL.name());
        }

        // Delete all pending invitation tokens before deleting memberships
        List<GroupMembership> allMemberships = memberships.findGroupMembershipsByStatus(groupId, MembershipStatus.PENDING);
        for (GroupMembership membership : allMemberships) {
            tokens.deleteActiveTokens(membership.getUser().getId(), TokenType.GROUP_INVITE);
        }

        memberships.deleteAllMemberships(g.getId());
        groups.delete(g);

        log.info("{} group deleted: groupId={} by={}", LOG_PREFIX, groupId, me.getEmail());
    }

    /**
     * Allows a member to leave a group.
     * OWNER cannot leave - they must delete the group or transfer ownership first.
     *
     * @param groupId group ID
     * @param currentEmailRaw user's email
     */
    @Transactional
    public void leaveGroup(Long groupId, String currentEmailRaw) {
        String email = normEmail(currentEmailRaw);
        log.info("{} leave group: groupId={}, email={}", LOG_PREFIX, groupId, email);

        Group g = accessControl.requireGroup(groupId);
        User me = accessControl.requireUser(email);

        if (g.getOwner().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GroupError.OWNER_CANNOT_LEAVE.name());
        }

        GroupMembership mem = memberships.findMembership(g.getId(), me.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        GroupError.NOT_A_MEMBER.name()
                ));

        if (mem.getStatus() != MembershipStatus.REMOVED) {
            mem.setStatus(MembershipStatus.REMOVED);
            memberships.save(mem);
        }

        tokens.deleteActiveTokens(me.getId(), TokenType.GROUP_INVITE);

        log.info("{} left group: groupId={}, userId={}", LOG_PREFIX, groupId, me.getId());
    }
}

