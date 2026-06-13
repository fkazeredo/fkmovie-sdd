package com.fksoft.application.booking.api;

import com.fksoft.application.booking.CancellationView;
import com.fksoft.application.booking.ReservationCancellationService;
import com.fksoft.shared.security.UserContextProvider;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer cancellation endpoint (SPEC-0018): the owner cancels a reservation. Ownership, the
 * cancellation window and refund handling live in {@link ReservationCancellationService}; 200 OK.
 */
@RestController
class ReservationCancelController {

    private final ReservationCancellationService cancellation;
    private final UserContextProvider userContext;

    ReservationCancelController(ReservationCancellationService cancellation, UserContextProvider userContext) {
        this.cancellation = cancellation;
        this.userContext = userContext;
    }

    @PostMapping("/api/reservations/{id}/cancel")
    CancellationView cancel(@PathVariable UUID id) {
        return cancellation.cancel(id, userContext.currentUser().userId());
    }
}
