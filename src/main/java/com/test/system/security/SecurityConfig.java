// src/main/java/com/test/system/security/SecurityConfig.java
package com.test.system.security;

import com.test.system.security.filter.AuthFilter;
import com.test.system.security.handler.GoogleOAuth2FailureHandler;
import com.test.system.security.handler.GoogleOAuth2SuccessHandler;
import com.test.system.service.authorization.oauth.GoogleOAuth2UserLoader;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.*;

import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Public endpoints (no authentication required).
     * IMPORTANT: Must stay in sync with SecurityWhitelist component.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            // Public API
            "/api/health",
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/verify",
            "/api/auth/verification/resend",
            "/api/auth/password/request-reset",
            "/api/auth/password/reset",
            "/api/auth/password/set",  // Password setup for new users (after invite acceptance)
            "/api/groups/invites/accept",  // Group invitation acceptance (from email links)
            "/swagger-ui/**",
            "/v3/api-docs/**",

            // OAuth2 public endpoints
            "/oauth2/**",
            "/oauth2/authorization/**",
            "/login/oauth2/**",

            // System endpoints
            "/error",
            "/favicon.ico"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider,
            AuthFilter authFilter
            // TODO: Re-enable OAuth2 when ClientRegistrationRepository is configured
            // GoogleOAuth2UserLoader googleOAuth2UserLoader,
            // GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler,
            // GoogleOAuth2FailureHandler googleOAuth2FailureHandler
    ) throws Exception {

        return http
                // CSRF protection for cookie-based JWT authentication
                .csrf(csrf -> csrf
                        .csrfTokenRepository(createCsrfTokenRepository())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                )
                .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // SPA: no redirects, just 401 for unauthorized
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                )

                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                .authenticationProvider(authenticationProvider)

                // Authentication filter handles both JWT and PAT tokens
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)

                // TODO: Re-enable OAuth2 when ClientRegistrationRepository is configured
                // OAuth2 Login (Google)
                // .oauth2Login(oauth -> oauth
                //         .authorizationEndpoint(ep -> ep.baseUri("/oauth2/authorization"))
                //         .redirectionEndpoint(ep -> ep.baseUri("/login/oauth2/code/*"))
                //         .userInfoEndpoint(ue -> ue.userService(googleOAuth2UserLoader))
                //         .successHandler(googleOAuth2SuccessHandler)
                //         .failureHandler(googleOAuth2FailureHandler)
                // )

                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    /**
     * Creates CSRF token repository configured for cross-origin SPA.
     * Cookie settings:
     * - HttpOnly: false (so JavaScript can read it)
     * - Path: / (available for all endpoints)
     * - SameSite: None (allows cross-origin POST requests for localhost development)
     * - Secure: false (for localhost HTTP development)
     *
     * NOTE: In production, use SameSite=Strict and Secure=true with HTTPS
     */
    private CookieCsrfTokenRepository createCsrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        repository.setCookieName("XSRF-TOKEN");
        // Configure cookie customizer for cross-origin support (localhost development)
        repository.setCookieCustomizer(cookieBuilder ->
            cookieBuilder
                .sameSite("None")  // Allow cross-origin requests (required for localhost:5173 → localhost:8083)
                .secure(false)     // Allow HTTP for localhost development (set to true in production with HTTPS)
                .path("/")         // Available for all paths
        );
        return repository;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "https://testforge.ca",
                "https://www.testforge.ca"
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*")); // Allow all headers for preflight
        cfg.setExposedHeaders(List.of("Location"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L); // Cache preflight response for 1 hour

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    /**
     * Custom CSRF token request handler for SPA applications.
     * This handler allows the CSRF token to be sent via header (X-XSRF-TOKEN)
     * instead of as a request parameter, which is the standard for SPAs.
     */
    static final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
        private final CsrfTokenRequestAttributeHandler delegate = new CsrfTokenRequestAttributeHandler();

        @Override
        public void handle(jakarta.servlet.http.HttpServletRequest request,
                          jakarta.servlet.http.HttpServletResponse response,
                          java.util.function.Supplier<org.springframework.security.web.csrf.CsrfToken> csrfToken) {
            // Always use XOR of token value for breach protection
            this.delegate.handle(request, response, csrfToken);
        }

        @Override
        public String resolveCsrfTokenValue(jakarta.servlet.http.HttpServletRequest request,
                                           org.springframework.security.web.csrf.CsrfToken csrfToken) {
            // Prefer header, but fall back to parameter
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            return (headerValue != null) ? headerValue : this.delegate.resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
