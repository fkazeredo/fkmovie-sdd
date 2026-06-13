package com.fksoft.application.payment;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** The webhook body is malformed or references an unknown payment/event (SPEC-0015): 422. */
public class InvalidWebhookPayloadException extends BusinessException {

    public InvalidWebhookPayloadException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "payment.invalid-payload");
    }
}
