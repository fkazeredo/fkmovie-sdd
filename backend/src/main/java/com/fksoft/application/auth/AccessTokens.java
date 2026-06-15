package com.fksoft.application.auth;

import java.time.Instant;

/**
 * Port for issuing signed access tokens (SPEC-0003, ADR 0005). The auth module owns the contract;
 * the JWT/HS256 implementation is a technical adapter in {@code com.fksoft.infra.security}, so the
 * domain depends on this interface and never on the crypto/framework details.
 */
public interface AccessTokens {

    /** Issues an access token for the given subject/role/tenant, valid from {@code now}. */
    IssuedAccessToken issue(String subject, String role, String tenantId, Instant now);

    /** The signed token and its expiry. */
    record IssuedAccessToken(String token, Instant expiresAt) {}
}
