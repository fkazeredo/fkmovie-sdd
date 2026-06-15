package com.fksoft.domain.payment;

import com.fksoft.domain.error.DomainException;

/** The webhook body is malformed or references an unknown payment/event (SPEC-0015): 422. */
public class InvalidWebhookPayloadException extends DomainException {

    public InvalidWebhookPayloadException() {
        super("payment.invalid-payload");
    }
}
