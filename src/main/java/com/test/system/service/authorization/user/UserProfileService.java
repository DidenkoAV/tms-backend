package com.test.system.service.authorization.user;

import com.test.system.dto.authorization.profile.UpdateProfileResponse;
import com.test.system.dto.authorization.profile.UserProfileResponse;
import com.test.system.dto.group.info.GroupInfoResponse;
import com.test.system.enums.auth.TokenType;
import com.test.system.exceptions.auth.UserNotFoundException;
import com.test.system.model.user.User;
import com.test.system.repository.user.UserRepository;
import com.test.system.repository.user.UserPreferencesRepository;
import com.test.system.service.authorization.core.EmailTokenService;
import com.test.system.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;

/**
 * Service for user profile management.
 * Handles profile retrieval, name updates, and email changes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private static final String LOG_PREFIX = "[UserProfile]";

    private final UserRepository users;
    private final UserPreferencesRepository userPreferences;
    private final EmailTokenService tokens;
    private final MailService mail;

    @Value("${app.auth.email-change-ttl-hours:24}")
    private int emailChangeTtlHours;

    /* ===================== Profile Retrieval ===================== */

    /**
     * Gets user by email.
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return users.findByEmail(normEmail(email))
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    /**
     * Gets user profile with all details.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        User user = users.findWithAllByEmail(normEmail(email))
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return buildProfileResponse(user);
    }

    /* ===================== Profile Update ===================== */

    /**
     * Updates user profile (name and/or email).
     */
    @Transactional
    public UpdateProfileResponse updateProfileComplete(String email, String newFullName, String newEmail) {
        User user = getUserByEmail(email);
        boolean nameUpdated = false;
        boolean emailChangeStarted = false;

        // Update full name if provided and different
        if (newFullName != null && !newFullName.isBlank()) {
            String trimmed = newFullName.trim();
            if (!trimmed.equals(user.getFullName())) {
                applyNewFullName(user, trimmed);
                nameUpdated = true;
            }
        }

        // Start email change if provided and different
        if (newEmail != null && !newEmail.isBlank()) {
            String normalized = normEmail(newEmail);
            if (!normalized.equalsIgnoreCase(user.getEmail())) {
                requestEmailChangeInternal(user, normalized, Duration.ofHours(emailChangeTtlHours));
                emailChangeStarted = true;
            }
        }

        User fresh = users.findWithAllById(user.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return new UpdateProfileResponse(
                "ok",
                nameUpdated,
                emailChangeStarted,
                buildProfileResponse(fresh)
        );
    }

    /* ===================== Full Name Update ===================== */

    @Transactional
    public void updateFullName(String currentEmail, String newFullName) {
        User user = users.findByEmail(normEmail(currentEmail))
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        applyNewFullName(user, newFullName);
    }

    private void applyNewFullName(User user, String newFullName) {
        String normalizedFullName = sanitizeName(newFullName);
        if (normalizedFullName.isEmpty()) {
            throw new IllegalArgumentException("Full name is required");
        }

        String prev = user.getFullName();
        user.setFullName(normalizedFullName);
        users.save(user);

        log.info("{} full name updated: userId={}, email={}, '{}' -> '{}'",
                LOG_PREFIX, user.getId(), user.getEmail(), safe(prev), normalizedFullName);
    }

    /* ===================== Email Change ===================== */

    @Transactional
    public void startEmailChange(String currentEmail, String newEmail) {
        User user = users.findByEmail(normEmail(currentEmail))
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        requestEmailChangeInternal(user, newEmail, Duration.ofHours(emailChangeTtlHours));
    }

    @Transactional
    public void confirmEmailChange(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }

        int dotIndex = rawToken.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex >= rawToken.length() - 1) {
            throw new IllegalArgumentException("Invalid token format");
        }

        String encodedEmailPart = rawToken.substring(dotIndex + 1);
        String nextEmail = decodeEmailFromToken(encodedEmailPart);

        if (nextEmail.isEmpty()) {
            throw new IllegalArgumentException("Token email is empty");
        }
        if (users.existsByEmail(nextEmail)) {
            throw new IllegalArgumentException("Email already in use");
        }

        var vt = tokens.validateAndConsumeToken(rawToken, TokenType.EMAIL_CHANGE);
        User user = vt.getUser();

        String oldEmail = user.getEmail();
        user.setEmail(nextEmail);
        users.save(user);

        log.info("{} email changed: userId={}, {} -> {}", LOG_PREFIX, user.getId(), oldEmail, nextEmail);
    }

    private void requestEmailChangeInternal(User user, String newEmail, Duration ttl) {
        String next = normEmail(newEmail);

        if (next.isEmpty()) {
            throw new IllegalArgumentException("New email is required");
        }
        if (user.getEmail().equalsIgnoreCase(next)) {
            throw new IllegalArgumentException("New email must be different");
        }
        if (users.existsByEmail(next)) {
            throw new IllegalArgumentException("Email already in use");
        }

        tokens.deleteActiveTokens(user.getId(), TokenType.EMAIL_CHANGE);

        String encodedEmail = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(next.getBytes(StandardCharsets.UTF_8));

        String rawToken = EmailTokenService.newRawToken() + "." + encodedEmail;

        tokens.createToken(user, TokenType.EMAIL_CHANGE, ttl, rawToken);
        mail.sendEmailChangeConfirm(next, user.getFullName(), rawToken);

        log.info("{} email change requested: userId={}, from={} -> to={}",
                LOG_PREFIX, user.getId(), user.getEmail(), next);
    }

    private String decodeEmailFromToken(String encoded) {
        try {
            return new String(
                    Base64.getUrlDecoder().decode(encoded),
                    StandardCharsets.UTF_8
            ).trim().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid token email encoding", ex);
        }
    }

    /* ===================== Profile Response Builder ===================== */

    private UserProfileResponse buildProfileResponse(User user) {
        var roles = user.getRoles().stream().map(r -> r.getName().name()).toList();
        var groups = user.getMemberships().stream()
                .filter(m -> m.getStatus().isActive())
                .map(m -> GroupInfoResponse.from(m.getGroup()))
                .toList();

        // Get current group from user preferences
        Long currentGroupId = userPreferences.findByUserId(user.getId())
                .map(prefs -> prefs.getCurrentGroup() != null ? prefs.getCurrentGroup().getId() : null)
                .orElse(null);

        return new UserProfileResponse(
                user.getId(), user.getEmail(), user.getFullName(), user.isEnabled(),
                roles, user.getCreatedAt(), currentGroupId, groups
        );
    }

    /* ===================== Helpers ===================== */

    private static String normEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String sanitizeName(String s) {
        return s == null ? "" : s.trim();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
