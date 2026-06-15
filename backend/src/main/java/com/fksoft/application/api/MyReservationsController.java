package com.fksoft.application.api;

import com.fksoft.domain.booking.MyReservationView;
import com.fksoft.domain.booking.MyReservationsService;
import com.fksoft.domain.booking.ReservationStatus;
import com.fksoft.infra.security.UserContextProvider;
import com.fksoft.infra.web.PageResponse;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The authenticated customer's reservation history (SPEC-0019). Owner scoping is mandatory (the
 * caller's id, never a parameter); filters and pagination live in {@link MyReservationsService}.
 */
@RestController
@RequiredArgsConstructor
class MyReservationsController {

    private final MyReservationsService myReservations;
    private final UserContextProvider userContext;

    @GetMapping("/api/me/reservations")
    PageResponse<MyReservationView> list(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(defaultValue = "false") boolean upcoming,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(
                myReservations.list(userContext.currentUser().userId(), status, upcoming, page, size),
                Function.identity());
    }
}
