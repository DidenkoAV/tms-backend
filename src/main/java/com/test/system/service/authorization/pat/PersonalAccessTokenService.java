package com.test.system.service.authorization.pat;

import com.test.system.dto.authorization.token.ApiTokenResponse;
import com.test.system.exceptions.auth.ForbiddenTokenAccessException;
import com.test.system.exceptions.auth.InvalidTokenException;
import com.test.system.exceptions.auth.TokenNotFoundException;
import com.test.system.exceptions.common.NotFoundException;
import com.test.system.model.auth.ApiToken;
import com.test.system.model.user.User;
import com.test.system.repository.auth.UserApiTokenRepository;
import com.test.system.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for managing Personal Access Tokens (PAT) for API authentication.
 * Tokens are long-lived credentials for CI/CD, scripts, and external integrations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalAccessTokenService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final String LOG_PREFIX = "[PAT]";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^pat_([A-Za-z0-9_-]+)\\.([A-Za-z0-9_-]+)$");

    private final UserApiTokenRepository repo;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /* ===================== Public API ===================== */

    /**
     * Creates a new Personal Access Token for the user.
     */
    @Transactional
    public ApiTokenResponse createToken(String email, String name, String scopes) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));

        String prefix = generateBase64Url(8);
        String secret = generateBase64Url(24);
        String fullToken = "pat_" + prefix + "." + secret;

        ApiToken token = ApiToken.builder()
                .user(user)
                .name(name == null || name.isBlank() ? "Token" : name.trim())
                .tokenPrefix(prefix)
                .secretHash(passwordEncoder.encode(secret))
                .scopes(scopes)
                .build();
        repo.save(token);

        log.info("{} created: userId={}, tokenId={}, prefix={}", LOG_PREFIX, user.getId(), token.getId(), prefix);
        return toResponse(token, fullToken);
    }

    /**
     * Lists all active tokens for the user.
     */
    @Transactional(readOnly = true)
    public List<ApiTokenResponse> listActiveTokens(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        return repo.findActiveTokensByUser(user.getId()).stream().map(t -> toResponse(t, null)).toList();
    }

    /**
     * Revokes a token by ID.
     */
    @Transactional
    public void revokeToken(String email, String tokenId) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        if (tokenId == null || tokenId.isBlank()) throw new IllegalArgumentException("Token ID is required");

        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        ApiToken token = repo.findById(UUID.fromString(tokenId)).orElseThrow(() -> new NotFoundException("Token not found"));

        if (!token.getUser().getId().equals(user.getId())) {
            log.warn("{} access denied: requesterUserId={}, tokenOwnerId={}, tokenId={}", LOG_PREFIX, user.getId(), token.getUser().getId(), token.getId());
            throw new ForbiddenTokenAccessException("You don't have permission to access this token");
        }

        if (token.getRevokedAt() == null) {
            token.setRevokedAt(Instant.now());
            log.info("{} revoked: userId={}, tokenId={}", LOG_PREFIX, user.getId(), token.getId());
        } else {
            log.debug("{} already revoked: userId={}, tokenId={}", LOG_PREFIX, user.getId(), token.getId());
        }
    }

    /**
     * Authenticates a token and returns the user's email.
     */
    @Transactional
    public String authenticateToken(String tokenString) {
        TokenParts parts = parseToken(tokenString);
        ApiToken token = repo.findActiveTokenByPrefix(parts.prefix()).orElseThrow(() -> {
            log.debug("{} not found or revoked: prefix={}", LOG_PREFIX, parts.prefix());
            return new TokenNotFoundException("Token not found or has been revoked");
        });

        if (!passwordEncoder.matches(parts.secret(), token.getSecretHash())) {
            log.debug("{} secret mismatch: tokenId={}", LOG_PREFIX, token.getId());
            throw new InvalidTokenException("Invalid token secret");
        }

        token.setLastUsedAt(Instant.now());
        String email = token.getUser().getEmail();
        log.debug("{} authenticated: prefix={}, email={}", LOG_PREFIX, parts.prefix(), email);
        return email;
    }

    /* ===================== Helpers ===================== */

    /**
     * Parses token string into prefix and secret parts.
     */
    private TokenParts parseToken(String tokenString) {
        if (tokenString == null || tokenString.isBlank()) {
            throw new InvalidTokenException("Token is required");
        }

        Matcher matcher = TOKEN_PATTERN.matcher(tokenString);
        if (!matcher.matches()) {
            log.debug("{} invalid format: token='{}'", LOG_PREFIX, tokenString);
            throw new InvalidTokenException("Invalid token format");
        }

        return new TokenParts(matcher.group(1), matcher.group(2));
    }

    /**
     * Generates random base64url string.
     */
    private static String generateBase64Url(int bytes) {
        byte[] buffer = new byte[bytes];
        RNG.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    /**
     * Maps token entity to response DTO.
     */
    private ApiTokenResponse toResponse(ApiToken token, String fullToken) {
        return new ApiTokenResponse(
                token.getId(),
                token.getName(),
                token.getCreatedAt(),
                token.getLastUsedAt(),
                token.getRevokedAt() != null,
                fullToken
        );
    }

    private record TokenParts(String prefix, String secret) {}
}
