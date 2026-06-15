package com.fksoft.domain.cinema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** SPEC-0007: seat domain validation and the architectural "no availability" assertion. */
class SeatTest {

    @Test
    void buildsAValidSeat() {
        var roomId = UUID.randomUUID();
        var seat = new Seat(roomId, "A", 1, SeatType.ACCESSIBLE);

        assertThat(seat.roomId()).isEqualTo(roomId);
        assertThat(seat.row()).isEqualTo("A");
        assertThat(seat.number()).isEqualTo(1);
        assertThat(seat.type()).isEqualTo(SeatType.ACCESSIBLE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a", "1", "AAA", "A1", " A"})
    void rejectsInvalidRow(String row) {
        assertThatThrownBy(() -> new Seat(UUID.randomUUID(), row, 1, SeatType.STANDARD))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveNumber(int number) {
        assertThatThrownBy(() -> new Seat(UUID.randomUUID(), "A", number, SeatType.STANDARD))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * A physical seat MUST NOT carry availability — that belongs to ScreeningSeat (SPEC-0011).
     * Asserted structurally so the rule survives refactors.
     */
    @Test
    void hasNoAvailabilityState() {
        var forbidden = List.of("avail", "status", "reserved", "occupied", "booked", "blocked");
        var instanceFieldNames = Stream.of(Seat.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(Field::getName)
                .map(String::toLowerCase)
                .toList();

        assertThat(instanceFieldNames)
                .as("Seat must have no availability-like field")
                .noneMatch(name -> forbidden.stream().anyMatch(name::contains));
    }
}
