package com.fksoft.application.screening;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** SPEC-0009: screening domain — derived end time, price invariant and cancel transition. */
class ScreeningTest {

    private static final Instant START = Instant.parse("2026-06-20T20:00:00Z");

    @Test
    void endsAtIsStartPlusDurationAndBuffer() {
        var screening = Screening.schedule(UUID.randomUUID(), UUID.randomUUID(), START, 117, 30, 3000);

        assertThat(screening.endsAt()).isEqualTo(START.plus(Duration.ofMinutes(147)));
        assertThat(screening.status()).isEqualTo(ScreeningStatus.SCHEDULED);
    }

    @Test
    void rejectsNonPositivePrice() {
        assertThatThrownBy(() -> Screening.schedule(UUID.randomUUID(), UUID.randomUUID(), START, 100, 30, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rescheduleRecomputesEndsAt() {
        var screening = Screening.schedule(UUID.randomUUID(), UUID.randomUUID(), START, 100, 30, 3000);
        var newStart = START.plus(Duration.ofHours(3));

        screening.reschedule(UUID.randomUUID(), UUID.randomUUID(), newStart, 90, 30, 4000);

        assertThat(screening.startsAt()).isEqualTo(newStart);
        assertThat(screening.endsAt()).isEqualTo(newStart.plus(Duration.ofMinutes(120)));
        assertThat(screening.basePriceCents()).isEqualTo(4000);
    }

    @Test
    void cancelTransitionsStatus() {
        var screening = Screening.schedule(UUID.randomUUID(), UUID.randomUUID(), START, 100, 30, 3000);

        screening.cancel();

        assertThat(screening.status()).isEqualTo(ScreeningStatus.CANCELLED);
        assertThat(screening.isScheduled()).isFalse();
    }
}
