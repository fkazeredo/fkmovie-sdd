package com.fksoft.application.api;

import com.fksoft.domain.booking.OperatorLookupService;
import com.fksoft.domain.booking.OperatorReservationDetailView;
import com.fksoft.domain.booking.OperatorReservationView;
import com.fksoft.infra.security.UserContextProvider;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operator reservation lookup endpoints (SPEC-0020): search by exactly one criterion and full detail.
 * Restricted to OPERATOR/ADMIN by the security chain ({@code /api/operator/**}); rules live in {@link
 * OperatorLookupService}.
 */
@RestController
class OperatorReservationController {

    private final OperatorLookupService lookup;
    private final UserContextProvider userContext;

    OperatorReservationController(OperatorLookupService lookup, UserContextProvider userContext) {
        this.lookup = lookup;
        this.userContext = userContext;
    }

    @GetMapping("/api/operator/reservations")
    List<OperatorReservationView> search(
            @RequestParam(required = false) String ticketCode,
            @RequestParam(required = false) UUID reservationId,
            @RequestParam(required = false) String email) {
        return lookup.search(userContext.currentUser().userId(), ticketCode, reservationId, email);
    }

    @GetMapping("/api/operator/reservations/{id}")
    OperatorReservationDetailView detail(@PathVariable UUID id) {
        return lookup.detail(id);
    }
}
