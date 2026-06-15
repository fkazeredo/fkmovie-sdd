package com.fksoft.domain.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** SPEC-0014: the FREE→HELD transition is the only place double booking could happen. */
class ScreeningSeatHoldTest {

    @Test
    void holdsAFreeSeat() {
        var seat = ScreeningSeat.free(UUID.randomUUID(), UUID.randomUUID());
        assertThat(seat.isFree()).isTrue();

        seat.hold();

        assertThat(seat.status()).isEqualTo(ScreeningSeatStatus.HELD);
        assertThat(seat.isFree()).isFalse();
    }

    @Test
    void rejectsHoldingASeatThatIsNotFree() {
        var seat = ScreeningSeat.free(UUID.randomUUID(), UUID.randomUUID());
        seat.hold();

        assertThatThrownBy(seat::hold).isInstanceOf(IllegalStateException.class);
    }
}
