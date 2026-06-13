package com.fksoft.application.payment;

import java.util.UUID;

/** The gateway's immediate refund response (SPEC-0015): a payment id and a PENDING status. */
public record RefundRequestResult(UUID paymentId, PaymentStatus status) {}
