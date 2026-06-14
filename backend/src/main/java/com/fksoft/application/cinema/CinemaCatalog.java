package com.fksoft.application.cinema;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public read API of the cinema module (SPEC-0009): the synchronous collaboration point other
 * modules use to learn about rooms and seats. This is the harmonizing implementation of the
 * read seam deferred in SPEC-0007 (see {@code architecture/simulation-and-mocking.md}).
 *
 * <p>Other modules depend on this facade and on {@link SeatView}/{@link SeatType} — never on the
 * {@code CinemaRoom}/{@code Seat} entities or their repositories (enforced by ArchUnit).
 */
@Service
public class CinemaCatalog {

    private final CinemaRoomRepository rooms;
    private final SeatRepository seats;

    CinemaCatalog(CinemaRoomRepository rooms, SeatRepository seats) {
        this.rooms = rooms;
        this.seats = seats;
    }

    public boolean roomExists(UUID roomId) {
        return rooms.existsById(roomId);
    }

    /** A room as a stable projection (empty if unknown). */
    @Transactional(readOnly = true)
    public Optional<RoomView> findRoom(UUID roomId) {
        return rooms.findById(roomId).map(room -> new RoomView(room.id(), room.name()));
    }

    /** All rooms as stable projections, ordered by name (for the admin screening scheduler). */
    @Transactional(readOnly = true)
    public List<RoomView> listRooms() {
        return rooms.findAll().stream()
                .map(room -> new RoomView(room.id(), room.name()))
                .sorted(Comparator.comparing(RoomView::name))
                .toList();
    }

    /** Physical seats of a room as a stable projection (empty if the room is unknown). */
    @Transactional(readOnly = true)
    public List<SeatView> seatsOf(UUID roomId) {
        return seats.findByRoomId(roomId).stream()
                .map(seat -> new SeatView(seat.id(), seat.row(), seat.number(), seat.type()))
                .toList();
    }
}
