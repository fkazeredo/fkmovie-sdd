package com.fksoft.application.payment;

import java.util.UUID;

/** The gateway's immediate response (SPEC-0015): a payment id and a PENDING status. */
public record PaymentRequestResult(UUID paymentId, PaymentStatus status) {}
