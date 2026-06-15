package com.fksoft.domain.screening;

/** Lifecycle of a screening (SPEC-0009). Deletion is replaced by {@code CANCELLED}. */
public enum ScreeningStatus {
    SCHEDULED,
    CANCELLED
}
