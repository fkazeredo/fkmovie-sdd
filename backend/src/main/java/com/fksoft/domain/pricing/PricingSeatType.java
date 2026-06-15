package com.fksoft.domain.pricing;

import com.fksoft.domain.cinema.SeatType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Admin-configurable surcharge (cents) for a seat type (SPEC-0012). The seat type is the key. */
@Entity
@Table(name = "pricing_seat_type")
public class PricingSeatType {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type")
    private SeatType seatType;

    @Column(name = "surcharge_cents", nullable = false)
    private int surchargeCents;

    protected PricingSeatType() {
        // JPA
    }

    PricingSeatType(SeatType seatType, int surchargeCents) {
        this.seatType = seatType;
        this.surchargeCents = surchargeCents;
    }

    void changeSurcharge(int surchargeCents) {
        this.surchargeCents = surchargeCents;
    }

    public SeatType seatType() {
        return seatType;
    }

    public int surchargeCents() {
        return surchargeCents;
    }
}
