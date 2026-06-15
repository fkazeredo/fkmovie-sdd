package com.fksoft.domain.pricing;

import com.fksoft.domain.cinema.SeatType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Admin-configurable surcharge (cents) for a seat type (SPEC-0012). The seat type is the key. */
@Entity
@Table(name = "pricing_seat_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PricingSeatType {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type")
    private SeatType seatType;

    @Column(name = "surcharge_cents", nullable = false)
    private int surchargeCents;

    PricingSeatType(SeatType seatType, int surchargeCents) {
        this.seatType = seatType;
        this.surchargeCents = surchargeCents;
    }

    void changeSurcharge(int surchargeCents) {
        this.surchargeCents = surchargeCents;
    }
}
