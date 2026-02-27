package com.test.system.service.group;

import com.test.system.component.group.GroupAccessControl;
import com.test.system.component.group.GroupMapper;
import com.test.system.dto.group.invitation.InviteAcceptResult;
import com.test.system.dto.group.member.GroupMemberResponse;
import com.test.system.enums.auth.RoleName;
import com.test.system.enums.auth.TokenType;
import com.test.system.enums.groups.GroupError;
import com.test.system.enums.groups.GroupRole;
import com.test.system.enums.groups.MembershipStatus;
import com.test.system.model.group.Group;
import com.test.system.model.group.GroupMembership;
import com.test.system.model.user.User;
import com.test.system.model.user.UserRole;
import com.test.system.repository.auth.UserRoleRepository;
import com.test.system.repository.group.GroupMembershipRepository;
import com.test.system.repository.user.UserRepository;
import com.test.system.service.authorization.core.EmailTokenService;
import com.test.system.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

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
    private final UserRepository users;
    private final UserRoleRepository roles;
    private final PasswordEncoder encoder;

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

        // Find or create invitee user
        User invitee = users.findByEmail(inviteeEmail)
                .orElseGet(() -> createPlaceholderUser(inviteeEmail));

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
     * Accepts a group invitation using the token from email (PUBLIC endpoint version).
     * Validates token and activates membership. Email validation is optional.
     *
     * @param rawToken the invitation token from email
     * @param currentEmailRaw optional email for additional validation (can be null for unauthenticated requests)
     * @return InviteAcceptResult with information about the acceptance
     */
    @Transactional
    public InviteAcceptResult acceptGroupInvitationPublic(String rawToken, String currentEmailRaw) {
        log.info("{} accept invite (public): email={}, token={}", LOG_PREFIX, currentEmailRaw, rawToken);

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

        // Optional email validation: if user is authenticated, verify it matches the token
        if (currentEmailRaw != null && !currentEmailRaw.isBlank()) {
            String currentEmail = normEmail(currentEmailRaw);
            if (!invitee.getEmail().equalsIgnoreCase(currentEmail)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GroupError.EMAIL_MISMATCH.name());
            }
        }

        // Check if this is a placeholder user (created during invitation)
        boolean wasPlaceholder = !invitee.isEnabled();

        // Enable placeholder users when they accept invitation
        // This allows them to complete registration or set password
        if (wasPlaceholder) {
            invitee.setEnabled(true);
            users.save(invitee);
            log.info("{} enabled placeholder user: userId={}, email={}", LOG_PREFIX, invitee.getId(), invitee.getEmail());
        }

        // Get group information for the result
        Group group = accessControl.requireGroup(groupId);

        GroupMembership existing = memberships.findMembership(groupId, invitee.getId()).orElse(null);
        if (existing != null && existing.getStatus() == MembershipStatus.ACTIVE) {
            log.info("{} accept invite no-op: already ACTIVE, groupId={}, userId={}",
                    LOG_PREFIX, groupId, invitee.getId());
            return buildAcceptResult(invitee, group, wasPlaceholder);
        }

        // Check limit for both new members and reactivating REMOVED members
        int activeCount = memberships.countMembershipsByStatus(groupId, MembershipStatus.ACTIVE);
        if (activeCount >= maxMembers) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GroupError.LIMIT_REACHED.name());
        }

        if (existing == null) {
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

        log.info("{} invite accepted: groupId={}, userId={}, wasPlaceholder={}",
                LOG_PREFIX, groupId, invitee.getId(), wasPlaceholder);

        return buildAcceptResult(invitee, group, wasPlaceholder);
    }

    /**
     * Accepts a group invitation using the token from email (AUTHENTICATED endpoint version).
     * Validates token, checks permissions, and activates membership.
     * Requires user to be authenticated.
     *
     * @deprecated Use {@link #acceptGroupInvitationPublic(String, String)} instead
     */
    @Transactional
    @Deprecated
    public void acceptGroupInvitation(String rawToken, String currentEmailRaw) {
        acceptGroupInvitationPublic(rawToken, currentEmailRaw);
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

    /**
     * Creates a placeholder user for invitations.
     * The user is created with a random password and disabled status.
     * They will need to complete registration when accepting the invitation.
     */
    private User createPlaceholderUser(String email) {
        var roleUser = roles.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER missing"));

        // Generate random password - user will set their own during registration
        String randomPassword = UUID.randomUUID().toString();

        User user = User.builder()
                .email(email)
                .password(encoder.encode(randomPassword))
                .fullName(email.split("@")[0]) // Use email prefix as temporary name
                .enabled(false) // Disabled until they complete registration
                .role(RoleName.ROLE_USER)
                .build();
        user.getRoles().add(roleUser);

        User saved = users.save(user);
        log.info("{} created placeholder user: userId={}, email={}", LOG_PREFIX, saved.getId(), saved.getEmail());

        return saved;
    }

    /**
     * Builds the result object for invitation acceptance.
     */
    private InviteAcceptResult buildAcceptResult(User user, Group group, boolean needsPassword) {
        return InviteAcceptResult.builder()
                .needsPassword(needsPassword)
                .email(user.getEmail())
                .groupName(group.getName())
                .groupId(group.getId())
                .build();
    }
}
