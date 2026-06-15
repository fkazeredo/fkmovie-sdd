package com.fksoft.domain.payment;

import com.fksoft.domain.error.DomainException;

/** The webhook HMAC signature is missing or does not match (SPEC-0015): 401, body not processed. */
public class InvalidWebhookSignatureException extends DomainException {

    public InvalidWebhookSignatureException() {
        super("payment.invalid-signature");
    }
}
