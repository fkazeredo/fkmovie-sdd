package com.fksoft.application.auth;

/** Disabling preserves history (SPEC-0003): disabled users cannot log in but are never deleted. */
public enum UserStatus {
    ACTIVE,
    DISABLED
}
