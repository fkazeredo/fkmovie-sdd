package com.fksoft.application.booking;

import com.fksoft.application.screening.ScreeningCatalog;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the authenticated customer's reservation history (SPEC-0019): owner-scoped, newest first,
 * optionally filtered by status and by upcoming sessions. Each row is enriched in batch through the
 * {@link ReservationSummaryAssembler} (no per-row N+1).
 */
@Service
public class MyReservationsService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final ReservationRepository reservations;
    private final ReservationSummaryAssembler summaries;
    private final ScreeningCatalog screenings;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    MyReservationsService(
            ReservationRepository reservations,
            ReservationSummaryAssembler summaries,
            ScreeningCatalog screenings,
            MeterRegistry meterRegistry,
            Clock clock) {
        this.reservations = reservations;
        this.summaries = summaries;
        this.screenings = screenings;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    /** Lists the caller's reservations (SPEC-0019); size clamped to 50, newest first. */
    @Transactional(readOnly = true)
    public Page<MyReservationView> list(UUID callerId, ReservationStatus status, boolean upcoming, int page, int size) {
        var sample = Timer.start(meterRegistry);
        try {
            var pageable =
                    PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
            var result = query(callerId, status, upcoming, pageable);
            var items = summaries.summarize(result.getContent()).stream()
                    .map(MyReservationsService::toView)
                    .toList();
            return new PageImpl<>(items, pageable, result.getTotalElements());
        } finally {
            sample.stop(Timer.builder("my_reservations_list_latency")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }

    private Page<Reservation> query(UUID callerId, ReservationStatus status, boolean upcoming, Pageable pageable) {
        if (upcoming) {
            var futureIds = screenings.futureScreeningIds(clock.instant());
            if (futureIds.isEmpty()) {
                return Page.empty(pageable);
            }
            return status == null
                    ? reservations.findByUserIdAndScreeningIdIn(callerId, futureIds, pageable)
                    : reservations.findByUserIdAndStatusAndScreeningIdIn(callerId, status, futureIds, pageable);
        }
        return status == null
                ? reservations.findByUserId(callerId, pageable)
                : reservations.findByUserIdAndStatus(callerId, status, pageable);
    }

    private static MyReservationView toView(ReservationSummaryAssembler.ReservationSummary summary) {
        return new MyReservationView(
                summary.reservationId(),
                summary.status(),
                summary.movieTitle(),
                summary.roomName(),
                summary.startsAt(),
                summary.totalCents(),
                summary.seatLabels());
    }

    private static int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
