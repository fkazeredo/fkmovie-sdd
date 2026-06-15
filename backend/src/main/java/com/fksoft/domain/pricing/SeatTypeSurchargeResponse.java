package com.fksoft.domain.pricing;

import com.fksoft.domain.cinema.SeatType;

/** Admin view of a seat-type surcharge (SPEC-0012). */
public record SeatTypeSurchargeResponse(SeatType seatType, int surchargeCents) {

    static SeatTypeSurchargeResponse from(PricingSeatType entity) {
        return new SeatTypeSurchargeResponse(entity.seatType(), entity.surchargeCents());
    }
}
