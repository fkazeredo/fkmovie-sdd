package com.fksoft.application.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * An audited reprint of a ticket by an operator (SPEC-0020). Append-only: one row per reprint, never a
 * ticket-state change. The count of rows for a ticket is shown to operators for fraud awareness.
 */
@Entity
@Table(name = "ticket_reprints")
public class TicketReprint {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "operator_user_id", nullable = false)
    private UUID operatorUserId;

    @Column(name = "reprinted_at", nullable = false)
    private Instant reprintedAt;

    protected TicketReprint() {
        // JPA
    }

    private TicketReprint(UUID ticketId, UUID operatorUserId, Instant reprintedAt) {
        this.id = UUID.randomUUID();
        this.tenantId = "default";
        this.ticketId = ticketId;
        this.operatorUserId = operatorUserId;
        this.reprintedAt = reprintedAt;
    }

    /** Records a reprint of a ticket by an operator (SPEC-0020). */
    public static TicketReprint record(UUID ticketId, UUID operatorUserId, Instant reprintedAt) {
        return new TicketReprint(ticketId, operatorUserId, reprintedAt);
    }

    public UUID id() {
        return id;
    }

    public UUID ticketId() {
        return ticketId;
    }
}
