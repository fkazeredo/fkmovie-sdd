package com.fksoft.infra.security;

/**
 * Port for the authenticated caller's identity (architecture/security.md): controllers and
 * application code MUST use this instead of touching {@code SecurityContextHolder} directly. The
 * adapter that reads the security context lives in {@code com.fksoft.infra.security} (ADR 0010/0011),
 * so the domain depends only on this interface, never on the framework.
 */
public interface UserContextProvider {

    /**
     * Returns the authenticated caller's identity.
     *
     * @throws IllegalStateException when called outside an authenticated request — protected
     *     endpoints are guarded by Spring Security, so this indicates a programming error.
     */
    UserContext currentUser();
}
