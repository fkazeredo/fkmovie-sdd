package com.fksoft.application.payment;

import com.fksoft.shared.error.DomainException;

/** The webhook HMAC signature is missing or does not match (SPEC-0015): 401, body not processed. */
public class InvalidWebhookSignatureException extends DomainException {

    public InvalidWebhookSignatureException() {
        super("payment.invalid-signature");
    }
}
