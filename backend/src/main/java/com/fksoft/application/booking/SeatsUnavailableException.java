package com.fksoft.application.booking;

import com.fksoft.shared.error.ApiErrorResponse;
import com.fksoft.shared.error.BusinessException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/**
 * One or more selected seats cannot be held (SPEC-0014): not FREE, or not part of the screening.
 * The response lists the offending seat ids under {@code fields} (all-or-nothing).
 */
public class SeatsUnavailableException extends BusinessException {

    private final transient List<UUID> seatIds;

    public SeatsUnavailableException(List<UUID> seatIds) {
        super(HttpStatus.CONFLICT, "booking.seats-unavailable");
        this.seatIds = List.copyOf(seatIds);
    }

    @Override
    public List<ApiErrorResponse.FieldViolation> fields() {
        return seatIds.stream()
                .map(id -> new ApiErrorResponse.FieldViolation("seatId", id.toString()))
                .toList();
    }
}
