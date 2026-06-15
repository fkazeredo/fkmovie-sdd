package com.fksoft.domain.cinema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fksoft.AbstractIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** SPEC-0007 acceptance: the Flyway seed (V7) creates the 4 rooms and typed seats. */
class CinemaSeedIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CinemaRoomRepository rooms;

    @Autowired
    private SeatRepository seats;

    @Test
    void seedsFourRoomsWithExpectedSeatCounts() {
        assertThat(rooms.count()).isEqualTo(4);
        assertThat(seatCountOf("Room 1")).isEqualTo(80);
        assertThat(seatCountOf("Room 2")).isEqualTo(80);
        assertThat(seatCountOf("Room 3")).isEqualTo(120);
        assertThat(seatCountOf("Room 4")).isEqualTo(48);
        assertThat(seats.count()).isEqualTo(328);
    }

    @Test
    void seatTypeDistributionMatchesSeedRules() {
        // total per room: VIP = whole last row; ACCESSIBLE/COMPANION = row A seats 1-2 / 3-4.
        assertRoomDistribution("Room 1", "H", 10, 66);
        assertRoomDistribution("Room 3", "J", 12, 104);
    }

    private void assertRoomDistribution(String roomName, String lastRow, int vipCount, long standardCount) {
        var roomSeats = seatsOf(roomName);

        assertThat(roomSeats.stream().filter(s -> s.type() == SeatType.ACCESSIBLE))
                .extracting(Seat::row, Seat::number)
                .containsExactlyInAnyOrder(tuple("A", 1), tuple("A", 2));

        assertThat(roomSeats.stream().filter(s -> s.type() == SeatType.COMPANION))
                .extracting(Seat::row, Seat::number)
                .containsExactlyInAnyOrder(tuple("A", 3), tuple("A", 4));

        assertThat(roomSeats.stream().filter(s -> s.type() == SeatType.VIP).toList())
                .hasSize(vipCount)
                .allMatch(s -> s.row().equals(lastRow));

        assertThat(roomSeats.stream().filter(s -> s.type() == SeatType.STANDARD).count())
                .isEqualTo(standardCount);
    }

    private long seatCountOf(String roomName) {
        return seats.countByRoomId(roomId(roomName));
    }

    private List<Seat> seatsOf(String roomName) {
        return seats.findByRoomId(roomId(roomName));
    }

    private java.util.UUID roomId(String roomName) {
        return rooms.findByName(roomName).orElseThrow().id();
    }
}
