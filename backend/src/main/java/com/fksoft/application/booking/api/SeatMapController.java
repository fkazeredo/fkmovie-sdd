package com.fksoft.application.booking.api;

import com.fksoft.application.booking.SeatMapResponse;
import com.fksoft.application.booking.SeatMapService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public seat map endpoint (SPEC-0011): anyone can view a screening's seat map. The live channel
 * (SPEC-0013) requires auth, but this REST read does not.
 */
@RestController
class SeatMapController {

    private final SeatMapService seatMapService;

    SeatMapController(SeatMapService seatMapService) {
        this.seatMapService = seatMapService;
    }

    @GetMapping("/api/screenings/{screeningId}/seats")
    SeatMapResponse seatMap(@PathVariable UUID screeningId) {
        return seatMapService.seatMap(screeningId);
    }
}
