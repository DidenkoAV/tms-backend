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
                .csrf(csrf -> csrf.disable())
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
}
