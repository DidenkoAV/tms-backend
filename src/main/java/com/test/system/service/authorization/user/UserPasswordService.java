package com.test.system.service.authorization.user;

import com.test.system.enums.auth.TokenType;
import com.test.system.exceptions.auth.InvalidPasswordException;
import com.test.system.exceptions.auth.PasswordRateLimitException;
import com.test.system.model.auth.PasswordChangeLog;
import com.test.system.model.user.User;
import com.test.system.repository.auth.PasswordChangeLogRepository;
import com.test.system.repository.user.UserRepository;
import com.test.system.security.policy.PasswordPolicy;
import com.test.system.service.authorization.core.EmailTokenService;
import com.test.system.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * Service for password management operations.
 * Handles password changes, resets, and validation with rate limiting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPasswordService {

    private static final String LOG_PREFIX = "[UserPassword]";
    private static final int PW_CHANGE_LIMIT_PER_24H = 3;

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final PasswordPolicy passwordPolicy;
    private final PasswordChangeLogRepository passwordChangeLogs;
    private final EmailTokenService tokens;
    private final MailService mail;

    /* ===================== Password Change ===================== */

    /**
     * Changes user password after validating current password and rate limits.
     */
    @Transactional
    public void changePassword(String emailRaw, String currentPassword, String newPassword) {
        String email = normEmail(emailRaw);
        User user = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        enforcePasswordChangeRateLimit(user);
        verifyCurrentPassword(user, currentPassword);
        ensureNewPasswordIsDifferent(user, newPassword);

        passwordPolicy.validate(newPassword, user.getEmail(), user.getFullName(), null);

        user.setPassword(encoder.encode(newPassword));
        users.save(user);
        savePasswordChangeLog(user);

        log.info("{} password changed: userId={}, email={}", LOG_PREFIX, user.getId(), user.getEmail());
    }

    /* ===================== Password Reset ===================== */

    /**
     * Initiates password reset flow by sending reset email.
     */
    @Transactional
    public void sendPasswordReset(String email, Duration ttl) {
        User user = users.findByEmail(normEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        tokens.deleteActiveTokens(user.getId(), TokenType.PASSWORD_RESET);

        String rawToken = EmailTokenService.newRawToken();
        tokens.createToken(user, TokenType.PASSWORD_RESET, ttl, rawToken);

        mail.sendPasswordReset(user.getEmail(), user.getFullName(), rawToken);
        log.info("{} password reset email sent: userId={}, email={}", LOG_PREFIX, user.getId(), user.getEmail());
    }

    /**
     * Completes password reset using reset token.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        var token = tokens.validateAndConsumeToken(rawToken, TokenType.PASSWORD_RESET);
        User user = token.getUser();

        passwordPolicy.validate(newPassword, user.getEmail(), user.getFullName(), null);
        user.setPassword(encoder.encode(newPassword));

        log.info("{} password reset completed: userId={}, email={}", LOG_PREFIX, user.getId(), user.getEmail());
    }

    /* ===================== Set Password (for placeholder users) ===================== */

    /**
     * Sets password for a placeholder user (created during invitation).
     * This is used when a user accepts an invitation and needs to set their password for the first time.
     *
     * @param emailRaw user's email
     * @param newPassword new password to set
     */
    @Transactional
    public void setPasswordForPlaceholderUser(String emailRaw, String setupToken, String newPassword) {
        if (setupToken == null || setupToken.isBlank()) {
            throw new IllegalArgumentException("PASSWORD_SET_TOKEN_REQUIRED");
        }

        var token = tokens.validateAndConsumeToken(setupToken, TokenType.PASSWORD_SET);
        User tokenUser = token.getUser();

        String email = normEmail(emailRaw);
        User user = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (!Objects.equals(user.getId(), tokenUser.getId())) {
            throw new IllegalArgumentException("TOKEN_USER_MISMATCH");
        }

        // Validate that this is actually a placeholder user scenario
        // (we don't enforce this strictly, but log it for monitoring)
        if (!user.isEnabled()) {
            log.warn("{} attempting to set password for disabled user: userId={}, email={}",
                    LOG_PREFIX, user.getId(), user.getEmail());
        }

        passwordPolicy.validate(newPassword, user.getEmail(), user.getFullName(), null);

        user.setPassword(encoder.encode(newPassword));
        user.setEnabled(true); // Ensure user is enabled
        users.save(user);
        savePasswordChangeLog(user);

        log.info("{} password set for placeholder user: userId={}, email={}",
                LOG_PREFIX, user.getId(), user.getEmail());
    }

    /* ===================== Private Helpers ===================== */

    private void enforcePasswordChangeRateLimit(User user) {
        Instant since = Instant.now().minusSeconds(24 * 3600);
        long used = passwordChangeLogs.countPasswordChangesSince(user.getId(), since);

        if (used >= PW_CHANGE_LIMIT_PER_24H) {
            log.warn("{} password change rate-limited: userId={}, email={}", LOG_PREFIX, user.getId(), user.getEmail());
            throw new PasswordRateLimitException("Password can be changed up to 3 times in 24 hours");
        }
    }

    private void verifyCurrentPassword(User user, String currentPassword) {
        if (!encoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }
    }

    private void ensureNewPasswordIsDifferent(User user, String newPassword) {
        if (encoder.matches(newPassword, user.getPassword())) {
            throw new InvalidPasswordException("New password must be different from current");
        }
    }

    private void savePasswordChangeLog(User user) {
        PasswordChangeLog logEntity = PasswordChangeLog.builder()
                .user(user)
                .createdAt(Instant.now())
                .build();
        passwordChangeLogs.save(logEntity);
    }

    private static String normEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
