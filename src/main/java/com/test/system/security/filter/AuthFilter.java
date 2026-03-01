package com.test.system.security.filter;

import com.test.system.dto.authorization.auth.TokenInfo;
import com.test.system.enums.auth.TokenSource;
import com.test.system.security.SecurityWhitelist;
import com.test.system.security.util.AuthTokenUtils;
import com.test.system.service.authorization.pat.PersonalAccessTokenService;
import com.test.system.service.authorization.core.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authentication filter for JWT (cookie/header) and PAT (header only) tokens.
 * Priority: Cookie → Authorization header. PAT identified by "pat_" prefix.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthFilter extends OncePerRequestFilter {

    private static final String X_FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";
    private static final String HTTPS_PROTOCOL = "https";

    private final SecurityWhitelist securityWhitelist;
    private final JwtService jwtService;
    private final PersonalAccessTokenService personalAccessTokenService;
    private final UserDetailsService userDetailsService;

    @Value("${app.auth.cookie.name:app_token}")
    private String cookieName;

    @Value("${app.auth.cookie.path:/}")
    private String cookiePath;

    @Value("${app.auth.cookie.domain:}")
    private String cookieDomain;

    @Value("${app.auth.cookie.same-site:Lax}")
    private String cookieSameSite;

    /** Skip authentication for public endpoints (see SecurityWhitelist). */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return securityWhitelist.shouldSkipAuthentication(
                request.getMethod(),
                request.getRequestURI()
        );
    }

    /** Main filter logic: extract token, determine type (JWT/PAT), and authenticate. */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        log.debug("[Auth] 🔍 Processing request: {} {}", request.getMethod(), requestUri);

        // Clear any existing authentication to prevent thread-local pollution
        SecurityContextHolder.clearContext();

        TokenInfo tokenInfo = extractTokenInfo(request);
        log.debug("[Auth] Token info: hasToken={}, source={}", tokenInfo.hasToken(), tokenInfo.source());

        if (tokenInfo.hasToken()) {
            if (AuthTokenUtils.isPersonalAccessToken(tokenInfo.token())) {
                // PAT: Long-lived token for API integrations (CI/CD, scripts, external services)
                log.debug("[Auth] Authenticating with PAT");
                authenticateWithPAT(tokenInfo.token());
            } else {
                // JWT: Short-lived token for web applications (browser, mobile apps)
                log.debug("[Auth] Authenticating with JWT");
                authenticateWithJWT(tokenInfo, request, response);
            }
        } else {
            log.debug("[Auth] No token found in request");
        }

        filterChain.doFilter(request, response);
    }

    /** Extract token from request. Priority: Cookie → Authorization header. */
    private TokenInfo extractTokenInfo(HttpServletRequest request) {
        // 1. Try cookie first (JWT only) - web applications
        String cookieToken = extractCookieToken(request);
        if (cookieToken != null) {
            return new TokenInfo(cookieToken, TokenSource.COOKIE);
        }

        // 2. Try Authorization header (JWT or PAT) - API clients, mobile apps, integrations
        String headerToken = AuthTokenUtils.extractBearerToken(request);
        if (headerToken != null) {
            return new TokenInfo(headerToken, TokenSource.HEADER);
        }

        return new TokenInfo(null, TokenSource.NONE);
    }

    /** Extract JWT token from cookie. */
    private String extractCookieToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            log.trace("[Auth] No cookies in request");
            return null;
        }

        log.trace("[Auth] Checking {} cookies", cookies.length);
        for (Cookie cookie : cookies) {
            log.trace("[Auth] Cookie: name='{}', value='{}', path='{}', domain='{}'",
                     cookie.getName(),
                     cookie.getValue() == null ? "null" : (cookie.getValue().isBlank() ? "EMPTY" : "***"),
                     cookie.getPath(),
                     cookie.getDomain());

            if (cookieName.equals(cookie.getName())) {
                if (cookie.getValue() == null || cookie.getValue().isBlank()) {
                    log.warn("[Auth] ⚠️ Found auth cookie but value is empty/null - ignoring");
                    return null;
                }
                log.debug("[Auth] Found valid auth cookie");
                return cookie.getValue();
            }
        }

        log.trace("[Auth] Auth cookie '{}' not found", cookieName);
        return null;
    }

    /** Authenticate using PAT token (stateful, validated against database). */
    private void authenticateWithPAT(String token) {
        try {
            // Validate PAT against database and update last_used_at timestamp
            String email = personalAccessTokenService.authenticateToken(token);
            UserDetails user = userDetailsService.loadUserByUsername(email);

            var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.debug("[Auth] PAT authenticated: {}", email);
        } catch (IllegalArgumentException e) {
            log.debug("[Auth] Invalid PAT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.warn("[Auth] PAT authentication failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }

    /** Authenticate using JWT token (stateless, validated using signature). */
    private void authenticateWithJWT(TokenInfo tokenInfo,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        String email = extractEmailFromJWT(tokenInfo, request, response);
        if (email != null) {
            authenticateUser(email, tokenInfo, request, response);
        }
    }

    /** Extract email from JWT token. Clears cookie if invalid and came from cookie. */
    private String extractEmailFromJWT(TokenInfo tokenInfo,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        try {
            // Validate JWT signature and extract subject (email)
            String email = jwtService.extractSubject(tokenInfo.token());
            log.debug("[Auth] JWT email extracted: {}", email);
            return email;
        } catch (Exception e) {
            log.warn("[Auth] ❌ Invalid JWT token: {} (source: {})", e.getMessage(), tokenInfo.source());

            // Clear cookie if JWT came from cookie (security measure)
            if (tokenInfo.isFromCookie()) {
                log.debug("[Auth] Clearing invalid cookie");
                clearCookie(request, response);
            }
            return null;
        }
    }

    /** Load user and set SecurityContext. Clears cookie if user not found and token from cookie. */
    private void authenticateUser(String email,
                                  TokenInfo tokenInfo,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        try {
            UserDetails user = userDetailsService.loadUserByUsername(email);

            // Check if user is enabled
            if (!user.isEnabled()) {
                log.warn("[Auth] User account is disabled (email not verified?): {}", email);
                if (tokenInfo.isFromCookie()) {
                    clearCookie(request, response);
                }
                return;
            }

            var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.info("[Auth] ✅ JWT authenticated successfully: email={}, authorities={}", email, user.getAuthorities());
        } catch (UsernameNotFoundException e) {
            log.warn("[Auth] ❌ User not found: {}", email);

            // Clear cookie if user not found and token came from cookie
            if (tokenInfo.isFromCookie()) {
                clearCookie(request, response);
            }
        } catch (Exception e) {
            log.error("[Auth] ❌ JWT authentication failed: email={}, error={}", email, e.getMessage(), e);
        }
    }

    /** Clear authentication cookie (used when JWT is invalid or user not found). */
    private void clearCookie(HttpServletRequest request, HttpServletResponse response) {
        log.debug("[Auth] Clearing authentication cookie");

        // Use maxAge=0 to explicitly expire cookie in browser
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure(request));
        cookie.setPath(cookiePath);
        cookie.setMaxAge(0);

        if (!cookieDomain.isBlank()) {
            cookie.setDomain(cookieDomain);
        }

        response.addCookie(cookie);
        log.debug("[Auth] Cookie cleared: name={}, path={}, domain={}", cookieName, cookiePath,
                  cookieDomain.isBlank() ? "none" : cookieDomain);
    }

    /** Check if request is secure (HTTPS). Checks X-Forwarded-Proto header and request.isSecure(). */
    private boolean isSecure(HttpServletRequest request) {
        String forwardedProto = request.getHeader(X_FORWARDED_PROTO_HEADER);
        return HTTPS_PROTOCOL.equalsIgnoreCase(forwardedProto) || request.isSecure();
    }

}
