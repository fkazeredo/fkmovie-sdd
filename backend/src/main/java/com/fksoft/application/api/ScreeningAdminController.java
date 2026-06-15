package com.fksoft.application.api;

import com.fksoft.application.api.dto.ScreeningRequest;
import com.fksoft.domain.screening.ScreeningResponse;
import com.fksoft.domain.screening.ScreeningService;
import com.fksoft.infra.security.UserContextProvider;
import com.fksoft.infra.web.PageResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin screening endpoints (SPEC-0009). Access is restricted to ROLE_ADMIN by the security chain
 * ({@code /api/admin/**}); business rules live in {@link ScreeningService}. The seat inventory /
 * map is the booking module's public endpoint (SPEC-0011), not this admin view.
 */
@RestController
@RequestMapping("/api/admin/screenings")
class ScreeningAdminController {

    private final ScreeningService screeningService;
    private final UserContextProvider userContext;

    ScreeningAdminController(ScreeningService screeningService, UserContextProvider userContext) {
        this.screeningService = screeningService;
        this.userContext = userContext;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ScreeningResponse create(@Valid @RequestBody ScreeningRequest request) {
        return screeningService.create(
                request.movieId(), request.roomId(), request.startsAt(), request.basePriceCents(), actingAdminId());
    }

    @GetMapping
    PageResponse<ScreeningResponse> list(
            @RequestParam(required = false) UUID roomId,
            @RequestParam(required = false) UUID movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(screeningService.list(roomId, movieId, from, to, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    ScreeningResponse get(@PathVariable UUID id) {
        return screeningService.get(id);
    }

    @PutMapping("/{id}")
    ScreeningResponse update(@PathVariable UUID id, @Valid @RequestBody ScreeningRequest request) {
        return screeningService.update(
                id, request.movieId(), request.roomId(), request.startsAt(), request.basePriceCents(), actingAdminId());
    }

    @PostMapping("/{id}/cancel")
    ScreeningResponse cancel(@PathVariable UUID id) {
        return screeningService.cancel(id, actingAdminId());
    }

    private UUID actingAdminId() {
        return userContext.currentUser().userId();
    }
}
