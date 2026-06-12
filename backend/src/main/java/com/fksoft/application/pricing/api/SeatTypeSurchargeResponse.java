package com.fksoft.application.pricing.api;

import com.fksoft.application.cinema.SeatType;
import com.fksoft.application.pricing.PricingSeatType;

/** Admin view of a seat-type surcharge (SPEC-0012). */
public record SeatTypeSurchargeResponse(SeatType seatType, int surchargeCents) {

    static SeatTypeSurchargeResponse from(PricingSeatType entity) {
        return new SeatTypeSurchargeResponse(entity.seatType(), entity.surchargeCents());
    }
}
