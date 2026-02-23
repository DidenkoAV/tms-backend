package com.test.system.service.authorization.user;

import com.test.system.dto.authorization.auth.AuthenticationResponse;
import com.test.system.enums.auth.TokenType;
import com.test.system.model.user.User;
import com.test.system.service.authorization.core.EmailTokenService;
import com.test.system.service.authorization.core.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;

import static com.test.system.utils.auth.AuthWebUtils.*;

/**
 * Service for user authentication operations.
 * <p>
 * Handles login, logout, JWT generation, and cookie management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAuthenticationService {

    private static final String LOG_PREFIX = "[UserAuth]";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailTokenService tokens;
    private final UserRegistrationService registrationService;
    private final UserPasswordService passwordService;

    @Value("${app.auth.cookie.name}")
    private String jwtCookieName;

    @Value("${app.auth.cookie.max-age-days}")
    private int jwtCookieMaxAgeDays;

    @Value("${app.auth.cookie.path}")
    private String jwtCookiePath;

    @Value("${app.auth.cookie.domain:}")
    private String jwtCookieDomain;

    @Value("${app.auth.cookie.same-site}")
    private String jwtCookieSameSite;

    @Value("${app.auth.verification-ttl-hours}")
    private int emailVerificationTtlHours;

    @Value("${app.auth.password-reset-ttl-hours}")
    private int passwordResetTtlHours;

    /* ===================== Registration & Verification ===================== */

    /**
     * Registers new user and sends email verification.
     */
    @Transactional
    public void registerNewUser(String email, String password, String fullName) {
        log.info("{} registering user: email={}", LOG_PREFIX, email);
        User user = registrationService.registerUser(email, password, fullName);
        registrationService.sendEmailVerification(user, Duration.ofHours(emailVerificationTtlHours));
    }

    /**
     * Verifies user email using verification token.
     */
    @Transactional
    public void verifyUserEmail(String verificationToken) {
        log.info("{} verifying email: token={}", LOG_PREFIX, redact(verificationToken));
        var token = tokens.validateAndConsumeToken(verificationToken, TokenType.EMAIL_VERIFY);
        registrationService.enableUser(token.getUser().getId());
    }

    /**
     * Resends email verification to user.
     */
    @Transactional
    public void resendEmailVerification(String email) {
        log.info("{} resending verification: email={}", LOG_PREFIX, email);
        registrationService.resendVerification(email, Duration.ofHours(emailVerificationTtlHours));
    }

    /* ===================== Login & Logout ===================== */

    /**
     * Authenticates user and returns JWT with HTTP-only cookie.
     */
    @Transactional
    public ResponseEntity<AuthenticationResponse> authenticateUser(
            String email,
            String password,
            HttpServletRequest request
    ) {
        log.info("{} authenticating user: email={}", LOG_PREFIX, email);

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        String jwt = jwtService.generateToken(email, Map.of());

        ResponseCookie cookie = buildJwtCookie(jwt, request, Duration.ofDays(jwtCookieMaxAgeDays));

        log.info("{} ✅ Login successful: email={}, cookie: name={}, path={}, secure={}, sameSite={}, domain={}",
                 LOG_PREFIX, email, jwtCookieName, jwtCookiePath, isSecure(request),
                 normalizeSameSite(jwtCookieSameSite), jwtCookieDomain.isBlank() ? "none" : jwtCookieDomain);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthenticationResponse(jwt, "Bearer"));
    }

    /**
     * Logs out user by clearing JWT cookie.
     */
    public ResponseEntity<Void> logoutUser(HttpServletRequest request) {
        log.info("{} logging out user", LOG_PREFIX);

        // Use maxAge=-1 to delete cookie immediately (more reliable than maxAge=0)
        ResponseCookie expiredCookie = ResponseCookie
                .from(jwtCookieName, "")
                .httpOnly(true)
                .secure(isSecure(request))
                .path(jwtCookiePath)
                .maxAge(-1)  // -1 = delete immediately
                .sameSite(normalizeSameSite(jwtCookieSameSite))
                .domain(jwtCookieDomain.isBlank() ? null : jwtCookieDomain)
                .build();

        log.info("{} cookie cleared for logout", LOG_PREFIX);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .build();
    }

    /* ===================== Password Reset ===================== */

    /**
     * Initiates password reset flow by sending reset email.
     */
    @Transactional
    public void initiatePasswordReset(String email) {
        log.info("{} password reset requested: email={}", LOG_PREFIX, email);
        passwordService.sendPasswordReset(email, Duration.ofHours(passwordResetTtlHours));
    }

    /**
     * Completes password reset using reset token.
     */
    @Transactional
    public void completePasswordReset(String resetToken, String newPassword) {
        log.info("{} completing password reset: token={}", LOG_PREFIX, redact(resetToken));
        passwordService.resetPassword(resetToken, newPassword);
    }

    /* ===================== Cookie Management ===================== */

    /**
     * Builds JWT cookie with security settings.
     */
    private ResponseCookie buildJwtCookie(String jwtValue, HttpServletRequest request, Duration maxAge) {
        return ResponseCookie
                .from(jwtCookieName, jwtValue)
                .httpOnly(true)
                .secure(isSecure(request))
                .path(jwtCookiePath)
                .maxAge(maxAge)
                .sameSite(normalizeSameSite(jwtCookieSameSite))
                .domain(jwtCookieDomain.isBlank() ? null : jwtCookieDomain)
                .build();
    }
}

