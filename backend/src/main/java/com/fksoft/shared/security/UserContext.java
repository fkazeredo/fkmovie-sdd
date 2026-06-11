package com.fksoft.shared.security;

import java.util.UUID;

/** Identity of the authenticated caller, extracted from the access token claims. */
public record UserContext(UUID userId, String role, String tenantId) {}
