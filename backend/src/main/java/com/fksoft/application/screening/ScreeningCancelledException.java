package com.fksoft.application.screening;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** The screening was cancelled — its seat map is gone (SPEC-0011): HTTP 410 Gone. */
public class ScreeningCancelledException extends BusinessException {

    public ScreeningCancelledException() {
        super(HttpStatus.GONE, "screening.cancelled");
    }
}
