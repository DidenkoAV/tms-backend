package com.test.system.service.authorization.oauth;

import com.test.system.service.authorization.user.UserRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

/**
 * Loads user information from Google OAuth2 and provisions local user account.
 * Called during Google OAuth2 authentication flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuth2UserLoader extends DefaultOAuth2UserService {

    private static final String LOG_PREFIX = "[GoogleOAuth2]";

    private final UserRegistrationService registrationService;

    /* ===================== OAuth2 User Loading ===================== */

    /**
     * Loads user from Google and provisions local user account.
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User googleUser = super.loadUser(request);

        Map<String, Object> attributes = googleUser.getAttributes();
        String providerId = request.getClientRegistration().getRegistrationId();

        log.debug("{} loaded user info: provider={}, attributes={}", LOG_PREFIX, providerId, attributes.keySet());

        String email = extractEmail(attributes);
        String displayName = extractDisplayName(attributes, email);

        validateEmail(email, providerId, attributes);
        provisionLocalUser(email, displayName);

        // Return original OAuth2User so success handler can access Google attributes
        return googleUser;
    }

    /* ===================== Attribute Extraction ===================== */

    /**
     * Extracts and normalizes email from Google attributes.
     */
    private String extractEmail(Map<String, Object> attributes) {
        String rawEmail = String.valueOf(attributes.getOrDefault("email", "")).trim();
        String normalizedEmail = rawEmail.toLowerCase(Locale.ROOT);
        log.trace("{} extracted email: '{}'", LOG_PREFIX, normalizedEmail);
        return normalizedEmail;
    }

    /**
     * Extracts display name from Google attributes with fallback to email.
     */
    private String extractDisplayName(Map<String, Object> attributes, String fallbackEmail) {
        // Try 'name' field first
        String fullName = String.valueOf(attributes.getOrDefault("name", "")).trim();
        if (!fullName.isEmpty()) {
            log.trace("{} display name from 'name': '{}'", LOG_PREFIX, fullName);
            return fullName;
        }

        // Try combining 'given_name' + 'family_name'
        String givenName = String.valueOf(attributes.getOrDefault("given_name", "")).trim();
        String familyName = String.valueOf(attributes.getOrDefault("family_name", "")).trim();
        String combinedName = (givenName + " " + familyName).trim();

        if (!combinedName.isEmpty()) {
            log.trace("{} display name from given+family: '{}'", LOG_PREFIX, combinedName);
            return combinedName;
        }

        // Fallback to email
        log.trace("{} display name fallback to email: '{}'", LOG_PREFIX, fallbackEmail);
        return fallbackEmail;
    }

    /* ===================== Validation & Provisioning ===================== */

    /**
     * Validates that email is present in Google attributes.
     */
    private void validateEmail(String email, String providerId, Map<String, Object> attributes) {
        if (email == null || email.isEmpty()) {
            log.warn("{} missing email in user info: provider={}, attributes={}", LOG_PREFIX, providerId, attributes);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_userinfo", "Email is required from OAuth2 provider", null),
                    "Email is required from OAuth2 provider"
            );
        }
    }

    /**
     * Creates or updates local user from Google account.
     */
    private void provisionLocalUser(String email, String displayName) {
        log.debug("{} provisioning local user: email='{}', name='{}'", LOG_PREFIX, email, displayName);
        registrationService.ensureUserFromGoogle(email, displayName);
    }
}
