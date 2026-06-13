package com.fksoft.application.booking;

import com.fksoft.application.pricing.HalfPriceCategory;
import com.fksoft.application.pricing.TicketType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * One held seat within a {@link Reservation} (SPEC-0014). Carries the chosen ticket type, the
 * snapshotted price (computed by pricing 0012 at hold time) and, for HALF tickets, the declared
 * half-price category and document reference (stored for the law's verification model).
 */
@Entity
@Table(name = "reservation_seats")
public class ReservationSeat {

    @Id
    private UUID id;

    @Column(name = "screening_seat_id", nullable = false)
    private UUID screeningSeatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", nullable = false)
    private TicketType ticketType;

    @Enumerated(EnumType.STRING)
    @Column(name = "half_price_category")
    private HalfPriceCategory halfPriceCategory;

    @Column(name = "document_reference")
    private String documentReference;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    protected ReservationSeat() {
        // JPA
    }

    ReservationSeat(
            UUID screeningSeatId,
            TicketType ticketType,
            HalfPriceCategory halfPriceCategory,
            String documentReference,
            int priceCents) {
        this.id = UUID.randomUUID();
        this.screeningSeatId = screeningSeatId;
        this.ticketType = ticketType;
        this.halfPriceCategory = halfPriceCategory;
        this.documentReference = documentReference;
        this.priceCents = priceCents;
    }

    public UUID id() {
        return id;
    }

    public UUID screeningSeatId() {
        return screeningSeatId;
    }

    public TicketType ticketType() {
        return ticketType;
    }

    public int priceCents() {
        return priceCents;
    }
}
