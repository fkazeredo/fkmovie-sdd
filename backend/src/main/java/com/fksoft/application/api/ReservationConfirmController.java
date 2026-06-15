package com.fksoft.application.api;

import com.fksoft.domain.booking.ReservationConfirmationService;
import com.fksoft.domain.booking.ReservationConfirmationView;
import com.fksoft.infra.security.UserContextProvider;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Purchase confirmation endpoint (SPEC-0016): the owner confirms a reservation, starting its async
 * payment. Ownership and state rules live in {@link ReservationConfirmationService}; 202 Accepted.
 */
@RestController
class ReservationConfirmController {

    private final ReservationConfirmationService confirmation;
    private final UserContextProvider userContext;

    ReservationConfirmController(ReservationConfirmationService confirmation, UserContextProvider userContext) {
        this.confirmation = confirmation;
        this.userContext = userContext;
    }

    @PostMapping("/api/reservations/{id}/confirm")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ReservationConfirmationView confirm(@PathVariable UUID id) {
        return confirmation.confirm(id, userContext.currentUser().userId());
    }
}
