package com.fksoft.domain.booking;

import com.fksoft.domain.cinema.SeatView;
import com.fksoft.domain.screening.ScreeningView;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Validated inputs for a reservation (SPEC-0014): the resolved screening, the seat map of its room
 * (by seat id) and the computed hold expiry. Produced by {@link ReservationPolicy} once the caller,
 * screening window and companion rule are checked; used by {@link ReservationService}.
 */
record ReservationContext(ScreeningView screening, Map<UUID, SeatView> seats, Instant expiresAt) {}
