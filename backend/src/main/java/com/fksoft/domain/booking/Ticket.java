package com.fksoft.domain.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A ticket issued for one held seat on purchase confirmation (SPEC-0016). Its {@code code}
 * (FKM-YYYY-NNNNNN) is globally unique; one ticket per reservation seat.
 */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "reservation_seat_id", nullable = false)
    private UUID reservationSeatId;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    protected Ticket() {
        // JPA
    }

    private Ticket(UUID reservationSeatId, String code, Instant issuedAt) {
        this.id = UUID.randomUUID();
        this.tenantId = "default";
        this.reservationSeatId = reservationSeatId;
        this.code = code;
        this.status = TicketStatus.VALID;
        this.issuedAt = issuedAt;
    }

    /** Issues a VALID ticket for a reservation seat (SPEC-0016). */
    public static Ticket issue(UUID reservationSeatId, String code, Instant issuedAt) {
        return new Ticket(reservationSeatId, code, issuedAt);
    }

    /** Cancels the ticket on reservation cancellation (SPEC-0018): {@code VALID → CANCELLED}. */
    public void cancel() {
        if (status != TicketStatus.VALID) {
            throw new IllegalStateException("Ticket " + id + " is not VALID: " + status);
        }
        status = TicketStatus.CANCELLED;
    }

    public UUID id() {
        return id;
    }

    public UUID reservationSeatId() {
        return reservationSeatId;
    }

    public String code() {
        return code;
    }

    public TicketStatus status() {
        return status;
    }
}
