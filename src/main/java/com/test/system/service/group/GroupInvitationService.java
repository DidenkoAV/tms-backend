package com.test.system.service.group;

import com.test.system.component.group.GroupAccessControl;
import com.test.system.component.group.GroupMapper;
import com.test.system.dto.group.member.GroupMemberResponse;
import com.test.system.enums.auth.TokenType;
import com.test.system.enums.groups.GroupError;
import com.test.system.enums.groups.GroupRole;
import com.test.system.enums.groups.MembershipStatus;
import com.test.system.model.group.Group;
import com.test.system.model.group.GroupMembership;
import com.test.system.model.user.User;
import com.test.system.repository.group.GroupMembershipRepository;
import com.test.system.service.authorization.core.EmailTokenService;
import com.test.system.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static com.test.system.component.group.GroupAccessControl.normEmail;
import static com.test.system.component.group.GroupAccessControl.safeName;

/**
 * Service for managing group invitations.
 * Handles invite, accept, cancel, and listing pending invitations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroupInvitationService {

    private static final String LOG_PREFIX = "[GroupInvite]";

    private final GroupMembershipRepository memberships;
    private final GroupAccessControl accessControl;
    private final EmailTokenService tokens;
    private final MailService mail;
    private final GroupMapper mapper;

    @Value("${app.groups.max-members:3}")
    private int maxMembers;

    @Value("${app.groups.invite.ttl-hours:72}")
    private int inviteTtlHours;

    /**
     * Invites a user to join a group.
     * Creates a PENDING membership and sends invitation email with token.
     *
     * @return created GroupMembership or null if user is already ACTIVE
     */
    @Transactional
    public GroupMembership inviteUserToGroup(Long groupId, String inviterEmailRaw, String inviteeEmailRaw) {
        String inviterEmail = normEmail(inviterEmailRaw);
        String inviteeEmail = normEmail(inviteeEmailRaw);

        // Validate email format before proceeding
        if (!isValidEmail(inviteeEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GroupError.INVALID_EMAIL.name());
        }

        log.info("{} invite: groupId={}, from={}, to={}", LOG_PREFIX, groupId, inviterEmail, inviteeEmail);

        Group group = accessControl.requireGroup(groupId);
        User inviter = accessControl.requireUser(inviterEmail);
        User invitee = accessControl.requireUser(inviteeEmail);

        // Cannot invite to personal groups
        if (group.isPersonal()) {
            log.warn("{} attempt to invite to personal group: groupId={}, inviter={}",
                    LOG_PREFIX, groupId, inviterEmail);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GroupError.CANNOT_INVITE_TO_PERSONAL.name());
        }

        accessControl.requireOwnerActiveMember(groupId, inviter.getId());

        int activeCount = memberships.countMembershipsByStatus(groupId, MembershipStatus.ACTIVE);
        if (activeCount >= maxMembers) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GroupError.LIMIT_REACHED.name());
        }

        boolean alreadyActive = memberships.hasMembershipWithStatus(
                groupId, invitee.getId(), MembershipStatus.ACTIVE);
        if (alreadyActive) {
            log.info("{} invite skipped: already ACTIVE, groupId={}, userId={}", LOG_PREFIX, groupId, invitee.getId());
            return null;
        }

        GroupMembership gm = memberships.findMembership(groupId, invitee.getId())
                .orElseGet(() -> GroupMembership.builder()
                        .group(group)
                        .user(invitee)
                        .role(GroupRole.MEMBER)
                        .createdAt(Instant.now())
                        .build());
        gm.setStatus(MembershipStatus.PENDING);
        gm.setInvitedBy(inviter);
        memberships.save(gm);

        String payload = base64(String.valueOf(groupId));
        String raw = EmailTokenService.newRawToken() + "." + payload;

        tokens.deleteActiveTokens(invitee.getId(), TokenType.GROUP_INVITE);
        tokens.createToken(invitee, TokenType.GROUP_INVITE, Duration.ofHours(inviteTtlHours), raw);

        mail.sendGroupInvite(
                invitee.getEmail(),
                safeName(invitee.getFullName(), invitee.getEmail()),
                safeName(inviter.getFullName(), inviter.getEmail()),
                group.getName(),
                raw
        );

        log.info("{} invite created: groupId={}, inviteeId={}, status=PENDING",
                LOG_PREFIX, groupId, invitee.getId());
        return gm;
    }

    /**
     * Accepts a group invitation using the token from email.
     * Validates token, checks permissions, and activates membership.
     */
    @Transactional
    public void acceptGroupInvitation(String rawToken, String currentEmailRaw) {
        String currentEmail = normEmail(currentEmailRaw);
        log.info("{} accept invite: email={}, token={}", LOG_PREFIX, currentEmail, rawToken);

        if (rawToken == null || rawToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GroupError.TOKEN_REQUIRED.name());
        }

        int dot = rawToken.lastIndexOf('.');
        if (dot <= 0 || dot >= rawToken.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GroupError.BAD_TOKEN_FORMAT.name());
        }

        Long groupId;
        try {
            String encoded = rawToken.substring(dot + 1);
            groupId = Long.valueOf(new String(
                    Base64.getUrlDecoder().decode(encoded),
                    StandardCharsets.UTF_8
            ));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GroupError.BAD_TOKEN_FORMAT.name());
        }

        var vt = tokens.validateAndConsumeToken(rawToken, TokenType.GROUP_INVITE);
        User invitee = vt.getUser();

        if (!invitee.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GroupError.EMAIL_MISMATCH.name());
        }

        GroupMembership existing = memberships.findMembership(groupId, invitee.getId()).orElse(null);
        if (existing != null && existing.getStatus() == MembershipStatus.ACTIVE) {
            log.info("{} accept invite no-op: already ACTIVE, groupId={}, userId={}",
                    LOG_PREFIX, groupId, invitee.getId());
            return;
        }

        // Check limit for both new members and reactivating REMOVED members
        int activeCount = memberships.countMembershipsByStatus(groupId, MembershipStatus.ACTIVE);
        if (activeCount >= maxMembers) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GroupError.LIMIT_REACHED.name());
        }

        if (existing == null) {
            Group group = accessControl.requireGroup(groupId);
            GroupMembership gm = GroupMembership.builder()
                    .group(group)
                    .user(invitee)
                    .role(GroupRole.MEMBER)
                    .status(MembershipStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();
            memberships.save(gm);
        } else {
            existing.setRole(GroupRole.MEMBER);
            existing.setStatus(MembershipStatus.ACTIVE);
            memberships.save(existing);
        }

        log.info("{} invite accepted: groupId={}, userId={}", LOG_PREFIX, groupId, invitee.getId());
    }

    /**
     * Cancels a pending invitation by setting status to REMOVED.
     * Only OWNER can cancel invitations.
     */
    @Transactional
    public void cancelPendingInvitation(Long membershipId, String requesterEmailRaw) {
        String requesterEmail = normEmail(requesterEmailRaw);
        log.info("{} cancel invite: membershipId={}, requester={}", LOG_PREFIX, membershipId, requesterEmail);

        GroupMembership gm = memberships.findById(membershipId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        GroupError.MEMBERSHIP_NOT_FOUND.name()
                ));
        Group group = gm.getGroup();
        User requester = accessControl.requireUser(requesterEmail);

        accessControl.requireOwnerActiveMember(group.getId(), requester.getId());

        if (gm.getStatus() != MembershipStatus.PENDING) {
            log.info("{} cancel invite no-op: membershipId={}, status={}", LOG_PREFIX, gm.getId(), gm.getStatus());
            return;
        }

        gm.setStatus(MembershipStatus.REMOVED);
        memberships.save(gm);

        tokens.deleteActiveTokens(gm.getUser().getId(), TokenType.GROUP_INVITE);

        log.info("{} invite cancelled: groupId={}, membershipId={}, byUserId={}",
                LOG_PREFIX, group.getId(), gm.getId(), requester.getId());
    }

    /**
     * Gets all pending invitations for a group.
     * Returns domain entities.
     */
    @Transactional(readOnly = true)
    public List<GroupMembership> getPendingInvitations(Long groupId, String requesterEmailRaw) {
        String requesterEmail = normEmail(requesterEmailRaw);
        log.info("{} list pending invites: groupId={}, requester={}", LOG_PREFIX, groupId, requesterEmail);

        Group group = accessControl.requireGroup(groupId);
        User requester = accessControl.requireUser(requesterEmail);

        accessControl.requireOwnerActiveMember(group.getId(), requester.getId());

        List<GroupMembership> pending = memberships.findGroupMembershipsByStatus(groupId, MembershipStatus.PENDING);
        log.debug("{} pending invites: groupId={}, count={}", LOG_PREFIX, groupId, pending.size());
        return pending;
    }

    /**
     * Gets all pending invitations for a group as DTOs.
     * Convenience method for controllers.
     */
    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getPendingInvitationsAsDto(Long groupId, String requesterEmailRaw) {
        return getPendingInvitations(groupId, requesterEmailRaw)
                .stream()
                .map(mapper::toMemberDto)
                .toList();
    }

    private static String base64(String v) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(v.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates email format using regex pattern.
     * Pattern matches the database constraint in users table.
     */
    private static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        // Same pattern as database constraint: ^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$
        String emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailPattern);
    }
}
