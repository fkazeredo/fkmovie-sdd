package com.fksoft.application.screening;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * The screening overlaps another SCHEDULED screening in the same room, considering the cleaning
 * buffer (SPEC-0009). Raised by the application pre-check and by the DB exclusion constraint.
 */
public class ScreeningRoomOverlapException extends BusinessException {

    public ScreeningRoomOverlapException() {
        super(HttpStatus.CONFLICT, "screening.room-overlap");
    }
}
