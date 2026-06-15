package com.fksoft.application.booking.api;

import com.fksoft.application.booking.MyReservationView;
import com.fksoft.application.booking.MyReservationsService;
import com.fksoft.application.booking.ReservationStatus;
import com.fksoft.shared.pagination.PageResponse;
import com.fksoft.shared.security.UserContextProvider;
import java.util.function.Function;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The authenticated customer's reservation history (SPEC-0019). Owner scoping is mandatory (the
 * caller's id, never a parameter); filters and pagination live in {@link MyReservationsService}.
 */
@RestController
class MyReservationsController {

    private final MyReservationsService myReservations;
    private final UserContextProvider userContext;

    MyReservationsController(MyReservationsService myReservations, UserContextProvider userContext) {
        this.myReservations = myReservations;
        this.userContext = userContext;
    }

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
