package com.fksoft.domain.booking;

import java.time.Instant;
import java.util.UUID;

/** A ticket was reprinted by an operator (SPEC-0020). Audit; published after commit. */
public record TicketReprinted(UUID ticketId, UUID operatorUserId, Instant occurredAt) {}
