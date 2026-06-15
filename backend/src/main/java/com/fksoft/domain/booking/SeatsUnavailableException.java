package com.fksoft.domain.booking;

import com.fksoft.domain.error.DomainException;
import com.fksoft.domain.error.ErrorDetails;
import java.util.List;
import java.util.UUID;

/**
 * One or more selected seats cannot be held (SPEC-0014): not FREE, or not part of the screening.
 * Carries the offending seat ids as domain details (all-or-nothing); the presentation renders them
 * into the response {@code fields}.
 */
public class SeatsUnavailableException extends DomainException implements ErrorDetails {

    private final transient List<UUID> seatIds;

    public SeatsUnavailableException(List<UUID> seatIds) {
        super("booking.seats-unavailable");
        this.seatIds = List.copyOf(seatIds);
    }

    @Override
    public List<Detail> details() {
        return seatIds.stream().map(id -> new Detail("seatId", id.toString())).toList();
    }
}
