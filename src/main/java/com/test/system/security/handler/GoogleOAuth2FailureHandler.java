package com.test.system.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Google OAuth2 Login Failure Handler.
 * Redirects to login page with error flag on Google authentication failure.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuth2FailureHandler implements AuthenticationFailureHandler {

    private static final String REDIRECT_LOGIN = "/login";
    private static final String QUERY_OAUTH_ERROR = "oauthError";
    private static final String ERROR_GENERIC = "1";

    @Value("${app.public-base-url:http://localhost:5173}")
    private String appPublicBaseUrl;

    /** Handle Google OAuth2 authentication failure. Redirects to login page with error flag. */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        String error = exception != null ? exception.getMessage() : "unknown";
        log.warn("[OAuth2] Google authentication failed: {}", error);

        String redirectUrl = buildErrorRedirectUrl();
        response.sendRedirect(redirectUrl);
    }

    /** Build error redirect URL with generic OAuth2 error flag. */
    private String buildErrorRedirectUrl() {
        return appPublicBaseUrl + REDIRECT_LOGIN + "?" + QUERY_OAUTH_ERROR + "=" + ERROR_GENERIC;
    }
}

