package com.fksoft.application.auth;

import java.time.Instant;
import java.util.UUID;

/** An invited user accepted and activated their account (SPEC-0005); internal audit event. */
public record UserInvitationAccepted(UUID userId, Instant occurredAt) {}
