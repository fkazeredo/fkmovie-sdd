package com.fksoft.application.screening.api;

import com.fksoft.application.screening.PublicScreeningView;
import com.fksoft.application.screening.PublicScreeningsService;
import com.fksoft.shared.pagination.PageResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public screenings listing (SPEC-0010): anonymous browse of upcoming sessions. Filtering, pagination
 * and the "from" price live in {@link PublicScreeningsService}; the handshake is permitted in security.
 */
@RestController
class PublicScreeningsController {

    private final PublicScreeningsService publicScreenings;

    PublicScreeningsController(PublicScreeningsService publicScreenings) {
        this.publicScreenings = publicScreenings;
    }

    @GetMapping("/api/screenings")
    PageResponse<PublicScreeningView> list(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) UUID movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return publicScreenings.list(date, movieId, page, size);
    }
}
