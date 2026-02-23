package com.test.system.enums.auth;

/** Enumeration of possible token sources in HTTP requests. */
public enum TokenSource {
    /** Token extracted from HTTP cookie (JWT only, web applications). */
    COOKIE,

    /** Token extracted from Authorization header (JWT or PAT, API clients). */
    HEADER,

    /** No token found in the request. */
    NONE
}

