package com.fksoft.shared.security;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Centralized access to the current user (architecture/security.md): application services
 * MUST use this instead of touching {@code SecurityContextHolder} directly.
 */
@Component
public class UserContextProvider {

    /**
     * Returns the authenticated caller's identity.
     *
     * @throws IllegalStateException when called outside an authenticated request — protected
     *     endpoints are guarded by Spring Security, so this indicates a programming error.
     */
    public UserContext currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new IllegalStateException("No authenticated user in the current context");
        }
        var jwt = jwtAuthentication.getToken();
        return new UserContext(
                UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("role"), jwt.getClaimAsString("tenantId"));
    }
}
