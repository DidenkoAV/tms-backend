package com.test.system.security.handler;

import com.test.system.model.user.User;
import com.test.system.service.authorization.core.JwtService;
import com.test.system.service.authorization.user.UserRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Google OAuth2 Login Success Handler.
 * Creates user account and issues JWT cookie after successful Google login.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    // Google OAuth2 attribute names
    private static final String ATTR_EMAIL = "email";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_GIVEN_NAME = "given_name";

    // Redirect paths
    private static final String REDIRECT_SUCCESS = "/main";
    private static final String REDIRECT_LOGIN = "/login";

    // Error query parameters
    private static final String QUERY_OAUTH_ERROR = "oauthError";
    private static final String ERROR_MISSING_EMAIL = "missing_email";

    // HTTP headers
    private static final String HEADER_X_FORWARDED_PROTO = "X-Forwarded-Proto";
    private static final String PROTOCOL_HTTPS = "https";

    // Cookie SameSite values
    private static final String SAME_SITE_NONE = "None";
    private static final String SAME_SITE_STRICT = "Strict";
    private static final String SAME_SITE_LAX = "Lax";

    private final JwtService jwtService;
    private final UserRegistrationService registrationService;

    @Value("${APP_PUBLIC_BASE_URL:http://localhost:5173}")
    private String appPublicBaseUrl;

    @Value("${app.auth.cookie.name:app_token}")
    private String cookieName;

    @Value("${app.auth.cookie.max-age-days:10}")
    private int cookieMaxAgeDays;

    @Value("${app.auth.cookie.path:/}")
    private String cookiePath;

    @Value("${app.auth.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Value("${app.auth.cookie.domain:}")
    private String cookieDomain;

    /** Handle successful Google OAuth2 authentication. Creates/updates user and issues JWT cookie. */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = extractEmail(principal);

        if (email == null || email.isBlank()) {
            log.warn("[OAuth2] Missing email in Google response");
            String redirectUrl = buildErrorRedirectUrl(ERROR_MISSING_EMAIL);
            response.sendRedirect(redirectUrl);
            return;
        }

        String name = extractName(principal);

        User user = registrationService.ensureUserFromGoogle(email, name);

        String jwt = jwtService.generateToken(user.getEmail().toLowerCase(), Map.of());
        ResponseCookie cookie = buildCookie(jwt, request);

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        response.sendRedirect(appPublicBaseUrl + REDIRECT_SUCCESS);

        log.info("[OAuth2] Google login successful: {}", email);
    }

    /** Extract email from Google OAuth2 response. */
    private String extractEmail(OAuth2User user) {
        if (user instanceof OidcUser oidc && oidc.getEmail() != null) {
            return oidc.getEmail().toLowerCase();
        }

        Object email = user.getAttributes().get(ATTR_EMAIL);
        return email != null ? email.toString().toLowerCase() : null;
    }

    /** Extract name from Google OAuth2 response. */
    private String extractName(OAuth2User user) {
        if (user instanceof OidcUser oidc) {
            String name = oidc.getFullName() != null ? oidc.getFullName() : oidc.getGivenName();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }

        Object name = user.getAttributes().get(ATTR_NAME);
        if (name != null && !name.toString().isBlank()) {
            return name.toString();
        }

        Object givenName = user.getAttributes().get(ATTR_GIVEN_NAME);
        return givenName != null ? givenName.toString() : "";
    }

    /** Build error redirect URL with error parameter. */
    private String buildErrorRedirectUrl(String errorCode) {
        return appPublicBaseUrl + REDIRECT_LOGIN + "?" + QUERY_OAUTH_ERROR + "=" + errorCode;
    }

    /** Build JWT cookie with configured settings. */
    private ResponseCookie buildCookie(String jwt, HttpServletRequest request) {
        boolean secure = isSecure(request);
        String sameSite = resolveSameSite(cookieSameSite);

        return ResponseCookie.from(cookieName, jwt)
                .httpOnly(true)
                .secure(secure)
                .path(cookiePath)
                .maxAge(Duration.ofDays(cookieMaxAgeDays))
                .sameSite(sameSite)
                .domain(cookieDomain.isBlank() ? null : cookieDomain)
                .build();
    }

    /** Check if request is secure (HTTPS). Checks X-Forwarded-Proto header and request.isSecure(). */
    private boolean isSecure(HttpServletRequest request) {
        String proto = request.getHeader(HEADER_X_FORWARDED_PROTO);
        return PROTOCOL_HTTPS.equalsIgnoreCase(proto) || request.isSecure();
    }

    /** Resolve SameSite cookie attribute from configuration. */
    private String resolveSameSite(String configured) {
        if (SAME_SITE_NONE.equalsIgnoreCase(configured)) {
            return SAME_SITE_NONE;
        }
        if (SAME_SITE_STRICT.equalsIgnoreCase(configured)) {
            return SAME_SITE_STRICT;
        }
        return SAME_SITE_LAX;
    }
}

