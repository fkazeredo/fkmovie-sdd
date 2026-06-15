package com.fksoft.application.api;

import com.fksoft.domain.screening.PublicScreeningView;
import com.fksoft.domain.screening.PublicScreeningsService;
import com.fksoft.infra.web.PageResponse;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public screenings listing (SPEC-0010): anonymous browse of upcoming sessions. Filtering, pagination
 * and the "from" price live in {@link PublicScreeningsService}; the handshake is permitted in security.
 */
@RestController
@RequiredArgsConstructor
class PublicScreeningsController {

    private final PublicScreeningsService publicScreenings;

    @GetMapping("/api/screenings")
    PageResponse<PublicScreeningView> list(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) UUID movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(publicScreenings.list(date, movieId, page, size), Function.identity());
    }
}
