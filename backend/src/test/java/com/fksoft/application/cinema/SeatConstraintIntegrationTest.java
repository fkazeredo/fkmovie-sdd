package com.fksoft.application.cinema;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fksoft.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * SPEC-0007: the {@code UNIQUE (room_id, seat_row, seat_number)} constraint rejects duplicate
 * seats in the same room. {@code @Transactional} rolls back so the shared Testcontainer keeps
 * the exact seed counts other tests assert on.
 */
@Transactional
class SeatConstraintIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CinemaRoomRepository rooms;

    @Autowired
    private SeatRepository seats;

    @Test
    void rejectsDuplicateRowAndNumberWithinSameRoom() {
        var room = rooms.saveAndFlush(new CinemaRoom("Constraint Room " + UUID.randomUUID()));
        seats.saveAndFlush(new Seat(room.id(), "A", 1, SeatType.STANDARD));

        assertThatThrownBy(() -> seats.saveAndFlush(new Seat(room.id(), "A", 1, SeatType.VIP)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
