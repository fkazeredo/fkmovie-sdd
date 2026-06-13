package com.fksoft.application.payment;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** The webhook HMAC signature is missing or does not match (SPEC-0015): 401, body not processed. */
public class InvalidWebhookSignatureException extends BusinessException {

    public InvalidWebhookSignatureException() {
        super(HttpStatus.UNAUTHORIZED, "payment.invalid-signature");
    }
}
