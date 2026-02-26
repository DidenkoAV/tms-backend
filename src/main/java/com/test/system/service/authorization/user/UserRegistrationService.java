package com.test.system.service.authorization.user;

import com.test.system.enums.auth.RoleName;
import com.test.system.enums.auth.TokenType;
import com.test.system.enums.groups.GroupRole;
import com.test.system.enums.groups.GroupType;
import com.test.system.enums.groups.MembershipStatus;
import com.test.system.model.group.Group;
import com.test.system.model.group.GroupMembership;
import com.test.system.model.user.User;
import com.test.system.repository.auth.UserRoleRepository;
import com.test.system.repository.group.GroupMembershipRepository;
import com.test.system.repository.group.GroupRepository;
import com.test.system.repository.user.UserRepository;
import com.test.system.service.authorization.core.EmailTokenService;
import com.test.system.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

/**
 * Service for user registration and email verification.
 * <p>
 * Handles new user registration, email verification, and OAuth2 user creation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {

    private static final String LOG_PREFIX = "[UserRegistration]";

    private final UserRepository users;
    private final UserRoleRepository roles;
    private final PasswordEncoder encoder;
    private final EmailTokenService tokens;
    private final MailService mail;
    private final GroupRepository groups;
    private final GroupMembershipRepository memberships;

    /* ===================== Registration ===================== */

    /**
     * Registers a new user with email and password.
     */
    @Transactional
    public User registerUser(String email, String rawPassword, String fullName) {
        String normEmail = normEmail(email);

        if (normEmail.isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (users.existsByEmail(normEmail)) {
            throw new IllegalArgumentException("Email already in use");
        }

        var roleUser = roles.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER missing"));

        User user = User.builder()
                .email(normEmail)
                .password(encoder.encode(rawPassword))
                .fullName(resolveFullNameOrDefault(fullName, normEmail))
                .enabled(false)
                .role(RoleName.ROLE_USER)
                .build();
        user.getRoles().add(roleUser);

        User saved = users.save(user);
        log.info("{} registered: userId={}, email={}", LOG_PREFIX, saved.getId(), saved.getEmail());

        ensurePersonalGroup(saved);
        return saved;
    }

    /**
     * Enables user account (after email verification).
     */
    @Transactional
    public void enableUser(Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.isEnabled()) {
            user.setEnabled(true);
            log.info("{} enabled: userId={}, email={}", LOG_PREFIX, user.getId(), user.getEmail());
        }
    }

    /* ===================== Email Verification ===================== */

    /**
     * Sends email verification token to user.
     */
    @Transactional
    public void sendEmailVerification(User user, Duration ttl) {
        tokens.deleteActiveTokens(user.getId(), TokenType.EMAIL_VERIFY);

        String rawToken = EmailTokenService.newRawToken();
        tokens.createToken(user, TokenType.EMAIL_VERIFY, ttl, rawToken);

        mail.sendEmailVerification(user.getEmail(), user.getFullName(), rawToken);
        log.info("{} verification email sent: userId={}, email={}", LOG_PREFIX, user.getId(), user.getEmail());
    }

    /**
     * Resends email verification to user.
     */
    @Transactional
    public void resendVerification(String email, Duration ttl) {
        User user = users.findByEmail(normEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.isEnabled()) {
            throw new IllegalArgumentException("Already verified");
        }

        sendEmailVerification(user, ttl);
        log.info("{} verification email re-sent: userId={}, email={}", LOG_PREFIX, user.getId(), user.getEmail());
    }

    /* ===================== OAuth2 / Google ===================== */

    /**
     * Ensures user exists from Google OAuth2 login (creates if needed).
     */
    @Transactional
    public User ensureUserFromGoogle(String emailRaw, String fullNameRaw) {
        String email = normEmail(emailRaw);
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        String fullName = resolveFullNameOrDefault(fullNameRaw, email);

        return users.findByEmail(email).orElseGet(() -> createGoogleUser(email, fullName));
    }

    private User createGoogleUser(String email, String fullName) {
        var roleUser = roles.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER missing"));

        String randomInternalPassword = encoder.encode(UUID.randomUUID().toString());

        User user = User.builder()
                .email(email)
                .password(randomInternalPassword)
                .fullName(fullName)
                .enabled(true)
                .role(RoleName.ROLE_USER)
                .build();
        user.getRoles().add(roleUser);

        User saved = users.save(user);
        log.info("{} OAuth2: user created from Google: userId={}, email={}",
                LOG_PREFIX, saved.getId(), saved.getEmail());

        ensurePersonalGroup(saved);
        return saved;
    }

    /* ===================== Personal Group Management ===================== */

    @Transactional
    public Group ensurePersonalGroup(User user) {
        Group existing = groups.findPersonalGroup(user.getId()).orElse(null);
        if (existing != null) {
            ensureOwnerMembership(existing, user);
            return existing;
        }

        String name = "Personal group for " + user.getEmail();

        Group group = Group.builder()
                .name(name)
                .owner(user)
                .groupType(GroupType.PERSONAL)
                .build();

        try {
            Group savedGroup = groups.save(group);

            log.info("{} group created (personal): groupId={}, ownerId={}, name='{}'",
                    LOG_PREFIX, savedGroup.getId(), user.getId(), savedGroup.getName());

            GroupMembership membership = GroupMembership.builder()
                    .group(savedGroup)
                    .user(user)
                    .role(GroupRole.OWNER)
                    .status(MembershipStatus.ACTIVE)
                    .build();
            memberships.save(membership);

            log.info("{} membership created: groupId={}, userId={}, role=OWNER, status=ACTIVE",
                    LOG_PREFIX, savedGroup.getId(), user.getId());

            return savedGroup;
        } catch (Exception e) {
            // Handle race condition: another thread may have created the personal group
            // Re-fetch and return the existing group
            log.warn("{} race condition detected while creating personal group for userId={}, retrying fetch",
                    LOG_PREFIX, user.getId());
            Group retryExisting = groups.findPersonalGroup(user.getId())
                    .orElseThrow(() -> new IllegalStateException("Failed to create or find personal group"));
            ensureOwnerMembership(retryExisting, user);
            return retryExisting;
        }
    }

    private void ensureOwnerMembership(Group group, User owner) {
        var opt = memberships.findMembership(group.getId(), owner.getId());
        if (opt.isPresent()) {
            GroupMembership membership = opt.get();
            if (membership.getRole() != GroupRole.OWNER || membership.getStatus() != MembershipStatus.ACTIVE) {
                GroupRole prevRole = membership.getRole();
                MembershipStatus prevStatus = membership.getStatus();

                membership.setRole(GroupRole.OWNER);
                membership.setStatus(MembershipStatus.ACTIVE);
                memberships.save(membership);

                log.info("{} membership repaired: groupId={}, userId={}, {}->OWNER, {}->ACTIVE",
                        LOG_PREFIX, group.getId(), owner.getId(), prevRole, prevStatus);
            }
        } else {
            GroupMembership membership = GroupMembership.builder()
                    .group(group)
                    .user(owner)
                    .role(GroupRole.OWNER)
                    .status(MembershipStatus.ACTIVE)
                    .build();
            memberships.save(membership);

            log.info("{} membership created: groupId={}, userId={}, role=OWNER, status=ACTIVE",
                    LOG_PREFIX, group.getId(), owner.getId());
        }
    }

    /* ===================== Helpers ===================== */

    private static String normEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String sanitizeName(String s) {
        return s == null ? "" : s.trim();
    }

    private static String resolveFullNameOrDefault(String fullNameRaw, String fallbackEmail) {
        String fullName = sanitizeName(fullNameRaw);
        return fullName.isEmpty() ? fallbackEmail : fullName;
    }
}

