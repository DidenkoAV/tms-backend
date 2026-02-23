package com.test.system.service.mail;

import com.test.system.service.HtmlTemplateService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Sends emails for authentication and group invitations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private static final String LOG_PREFIX = "[Mail]";

    private final JavaMailSender sender;
    private final HtmlTemplateService htmlTemplates;

    @Value("${app.api-base-url:http://localhost:8083}")
    private String apiBaseUrl;

    @Value("${app.public-base-url:http://localhost:5173}")
    private String appPublicBaseUrl;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${app.mail.from}")
    private String mailFrom;

    // ========================================================================
    // Public API
    // ========================================================================

    /**
     * Email verification (new account).
     */
    public void sendEmailVerification(String to, String fullName, String rawToken) {
        String link = buildLink("/api/auth/verify", rawToken);
        String subject = "Confirm your email";
        String html = htmlTemplates.emailVerificationEmail(fullName, link);

        log.info("{} Sending email verification to={}, token={}",
                LOG_PREFIX, redactEmail(to), redactToken(rawToken));
        sendHtml(to, subject, html);
        log.info("{} Email verification sent to={}", LOG_PREFIX, redactEmail(to));
    }

    /**
     * Password reset.
     */
    public void sendPasswordReset(String to, String fullName, String rawToken) {
        String link = buildLink("/api/auth/password/reset", rawToken);
        String subject = "Reset your password";
        String html = htmlTemplates.passwordResetEmail(fullName, link);

        log.info("{} Sending password reset to={}, token={}",
                LOG_PREFIX, redactEmail(to), redactToken(rawToken));
        sendHtml(to, subject, html);
        log.info("{} Password reset sent to={}", LOG_PREFIX, redactEmail(to));
    }

    /**
     * Confirm new email (profile email change).
     */
    public void sendEmailChangeConfirm(String toNewEmail, String fullName, String rawToken) {
        String link = buildLink("/api/auth/email/confirm", rawToken);
        String subject = "Confirm your new email";
        String html = htmlTemplates.emailChangeConfirmEmail(fullName, link);

        log.info("{} Sending email change confirm toNew={}, token={}",
                LOG_PREFIX, redactEmail(toNewEmail), redactToken(rawToken));
        sendHtml(toNewEmail, subject, html);
        log.info("{} Email change confirm sent toNew={}", LOG_PREFIX, redactEmail(toNewEmail));
    }

    /**
     * Group invitation (SPA deep link).
     */
    public void sendGroupInvite(
            String to,
            String inviteeName,
            String groupName,
            String inviterName,
            String rawToken
    ) {
        String link = buildAppLink("/invite/accept", rawToken);
        String subject = "You've been invited to join a group";

        String safeInviteeName = (inviteeName == null || inviteeName.isBlank()) ? to : inviteeName;
        String html = htmlTemplates.groupInviteEmail(safeInviteeName, groupName, inviterName, link);

        log.info("{} Sending group invite to={}, inviter={}, group={}, token={}",
                LOG_PREFIX, redactEmail(to), inviterName, groupName, redactToken(rawToken));
        sendHtml(to, subject, html);
        log.info("{} Group invite sent to={}", LOG_PREFIX, redactEmail(to));
    }

    // ========================================================================
    // Link builders
    // ========================================================================

    private String buildLink(String path, String rawToken) {
        String token = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        return apiBaseUrl + path + "?token=" + token;
    }

    private String buildAppLink(String path, String rawToken) {
        String token = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        return appPublicBaseUrl + path + "?token=" + token;
    }

    // ========================================================================
    // Low-level send helper
    // ========================================================================

    private void sendHtml(String to, String subject, String html) {
        if (!mailEnabled) {
            log.info("{} Email sending is DISABLED (dev mode). Would send to={}, subject={}",
                    LOG_PREFIX, redactEmail(to), subject);
            return;
        }

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            log.trace("{} Sending MIME message: from={}, to={}, subject={}",
                    LOG_PREFIX, mailFrom, redactEmail(to), subject);

            sender.send(message);
        } catch (Exception e) {
            log.error("{} Failed to send email: to={}, subject={}, reason={}",
                    LOG_PREFIX, redactEmail(to), subject, e.getMessage(), e);
            throw new RuntimeException("Failed to send mail: " + e.getMessage(), e);
        }
    }

    // ========================================================================
    // Small helpers
    // ========================================================================

    private static String redactToken(String token) {
        if (token == null) return "";
        String t = token.trim();
        if (t.length() <= 10) return "***";
        return t.substring(0, 6) + "...";
    }

    private static String redactEmail(String email) {
        if (email == null || email.isBlank()) return "";
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String prefix = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return prefix + "***" + domain;
    }
}

