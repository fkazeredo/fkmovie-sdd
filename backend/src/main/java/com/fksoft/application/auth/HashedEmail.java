package com.fksoft.application.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Pseudonymized email for log statements (SPEC-0003 observability: no PII in the log
 * aggregator, but attempts for the same email must still be correlatable).
 */
final class HashedEmail {

    private HashedEmail() {}

    static String of(String email) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(email.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
