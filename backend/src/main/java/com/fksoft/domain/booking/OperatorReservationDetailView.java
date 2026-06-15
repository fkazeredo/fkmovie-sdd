package com.fksoft.domain.booking;

/**
 * Operator detail of a reservation (SPEC-0020): the full {@link ReservationView} plus the customer's
 * name and full email (the operator legitimately needs it to verify identity at the counter).
 */
public record OperatorReservationDetailView(ReservationView reservation, String customerName, String customerEmail) {}
