package com.fksoft.application.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** SPEC-0018: the confirmed-cancellation window is open while at least 2h remain (>= boundary passes). */
class CancellationWindowTest {

    private static final Instant NOW = Instant.parse("2026-06-13T12:00:00Z");
    private static final Duration WINDOW = Duration.ofHours(2);

    @Test
    void exactlyAtTheBoundaryIsOpen() {
        var startsAt = NOW.plus(Duration.ofHours(2));
        assertThat(ReservationCancellationService.cancellationWindowOpen(startsAt, NOW, WINDOW))
                .isTrue();
    }

    @Test
    void oneSecondInsideTheWindowIsClosed() {
        var startsAt = NOW.plus(Duration.ofHours(2)).minusSeconds(1);
        assertThat(ReservationCancellationService.cancellationWindowOpen(startsAt, NOW, WINDOW))
                .isFalse();
    }

    @Test
    void wellOutsideTheWindowIsOpen() {
        var startsAt = NOW.plus(Duration.ofHours(2)).plusSeconds(1);
        assertThat(ReservationCancellationService.cancellationWindowOpen(startsAt, NOW, WINDOW))
                .isTrue();
    }
}
