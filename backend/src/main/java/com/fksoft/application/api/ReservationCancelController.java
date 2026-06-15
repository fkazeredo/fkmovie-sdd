package com.fksoft.application.api;

import com.fksoft.domain.booking.CancellationView;
import com.fksoft.domain.booking.ReservationCancellationService;
import com.fksoft.infra.security.UserContextProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer cancellation endpoint (SPEC-0018): the owner cancels a reservation. Ownership, the
 * cancellation window and refund handling live in {@link ReservationCancellationService}; 200 OK.
 */
@RestController
@RequiredArgsConstructor
class ReservationCancelController {

    private final ReservationCancellationService cancellation;
    private final UserContextProvider userContext;

    @PostMapping("/api/reservations/{id}/cancel")
    CancellationView cancel(@PathVariable UUID id) {
        return cancellation.cancel(id, userContext.currentUser().userId());
    }
}
