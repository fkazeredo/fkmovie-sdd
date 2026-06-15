package com.fksoft.domain.payment;

import java.util.UUID;

/** A charge settled successfully (SPEC-0015). Booking confirms the reservation (0016). */
public record PaymentSucceeded(UUID paymentId, UUID reservationId, int amountCents) {}
