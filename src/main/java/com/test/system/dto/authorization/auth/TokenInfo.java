package com.test.system.dto.authorization.auth;

import com.test.system.enums.auth.TokenSource;

/**
 * Token information holder containing token value and its source (cookie or header).
 *
 * @param token  the authentication token value (JWT or PAT), or null if no token found
 * @param source the source from which the token was extracted
 */
public record TokenInfo(String token, TokenSource source) {

    /** Check if token is present. */
    public boolean hasToken() {
        return token != null;
    }

    /** Check if token was extracted from cookie. */
    public boolean isFromCookie() {
        return source == TokenSource.COOKIE;
    }

    /** Check if token was extracted from header. */
    public boolean isFromHeader() {
        return source == TokenSource.HEADER;
    }
}

