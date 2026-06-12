package com.fksoft.application.screening.api;

import com.fksoft.application.screening.Screening;
import com.fksoft.application.screening.ScreeningStatus;
import java.time.Instant;
import java.util.UUID;

/** Admin-facing screening view (SPEC-0009). {@code endsAt} is the derived end of the session. */
public record ScreeningResponse(
        UUID id,
        UUID movieId,
        UUID roomId,
        Instant startsAt,
        Instant endsAt,
        int basePriceCents,
        ScreeningStatus status) {

    /** Maps a screening entity to its admin-facing view. */
    public static ScreeningResponse from(Screening screening) {
        return new ScreeningResponse(
                screening.id(),
                screening.movieId(),
                screening.roomId(),
                screening.startsAt(),
                screening.endsAt(),
                screening.basePriceCents(),
                screening.status());
    }
}
