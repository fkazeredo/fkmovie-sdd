package com.fksoft.domain.screening;

/**
 * Lifecycle of a catalog movie (SPEC-0008). {@code ARCHIVED} hides it from screening creation
 * (0009) and the public list (0010) without breaking existing screenings.
 */
public enum MovieStatus {
    ACTIVE,
    ARCHIVED
}
