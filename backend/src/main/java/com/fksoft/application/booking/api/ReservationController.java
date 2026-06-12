package com.fksoft.application.booking.api;

import com.fksoft.application.booking.ReservationService;
import com.fksoft.application.booking.ReservationView;
import com.fksoft.application.booking.SeatSelection;
import com.fksoft.shared.security.UserContextProvider;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reservation endpoints (SPEC-0014). Creating a reservation requires a CUSTOMER (enforced by the
 * security chain); the verified-email and business rules live in {@link ReservationService}.
 * Reading is owner- or staff-only (checked in the service).
 */
@RestController
class ReservationController {

    private final ReservationService reservations;
    private final UserContextProvider userContext;

    ReservationController(ReservationService reservations, UserContextProvider userContext) {
        this.reservations = reservations;
        this.userContext = userContext;
    }

    @PostMapping("/api/screenings/{screeningId}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    ReservationView reserve(@PathVariable UUID screeningId, @Valid @RequestBody CreateReservationRequest request) {
        var caller = userContext.currentUser();
        List<SeatSelection> selections = request.seats().stream()
                .map(seat -> new SeatSelection(
                        seat.seatId(), seat.ticketType(), seat.halfPriceCategory(), seat.documentReference()))
                .toList();
        return reservations.reserve(caller.userId(), screeningId, selections);
    }

    @GetMapping("/api/reservations/{id}")
    ReservationView get(@PathVariable UUID id) {
        var caller = userContext.currentUser();
        return reservations.get(id, caller.userId(), caller.role());
    }
}
