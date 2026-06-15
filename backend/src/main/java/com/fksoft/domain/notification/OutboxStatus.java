package com.fksoft.domain.notification;

/** Lifecycle of an outbox email (SPEC-0006). FAILED_PERMANENT is the dead-letter state. */
public enum OutboxStatus {
    PENDING,
    SENT,
    FAILED_PERMANENT
}
