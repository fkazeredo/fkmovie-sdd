package com.fksoft.application.payment;

import java.util.UUID;

/** A charge failed (SPEC-0015). Booking cancels the reservation and releases seats (0016). */
public record PaymentFailed(UUID paymentId, UUID reservationId, int amountCents) {}
