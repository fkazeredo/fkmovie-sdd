package com.fksoft.infra.realtime;

import java.security.Principal;

/**
 * The authenticated STOMP user (SPEC-0013): name is the JWT subject (userId), used by Spring to
 * route {@code /user/**} destinations to the right session.
 */
record StompPrincipal(String userId) implements Principal {

    @Override
    public String getName() {
        return userId;
    }
}
