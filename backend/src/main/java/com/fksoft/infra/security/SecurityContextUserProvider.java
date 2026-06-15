package com.fksoft.infra.security;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing the {@link UserContextProvider} port (ADR 0011): reads the authenticated
 * caller from Spring Security's {@code SecurityContextHolder} / JWT. This is the only place that
 * touches the security framework; the domain depends on the port, never on this class.
 */
@Component
class SecurityContextUserProvider implements UserContextProvider {

    @Override
    public UserContext currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new IllegalStateException("No authenticated user in the current context");
        }
        var jwt = jwtAuthentication.getToken();
        return new UserContext(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("role"));
    }
}
