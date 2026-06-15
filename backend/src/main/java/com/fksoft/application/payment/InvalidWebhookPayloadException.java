package com.fksoft.application.payment;

import com.fksoft.shared.error.DomainException;

/** The webhook body is malformed or references an unknown payment/event (SPEC-0015): 422. */
public class InvalidWebhookPayloadException extends DomainException {

    public InvalidWebhookPayloadException() {
        super("payment.invalid-payload");
    }
}
