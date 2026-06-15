package com.fksoft.infra.security;

import java.util.UUID;

/** Identity of the authenticated caller, extracted from the access token claims. */
public record UserContext(UUID userId, String role) {}
