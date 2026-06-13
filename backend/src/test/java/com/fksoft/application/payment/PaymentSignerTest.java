package com.fksoft.application.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** SPEC-0015: HMAC signing is deterministic and verification is exact (constant-time). */
class PaymentSignerTest {

    private final PaymentSigner signer = new PaymentSigner("top-secret-key");

    @Test
    void signsDeterministicallyAndVerifies() {
        var body = "{\"eventType\":\"PAYMENT_SUCCEEDED\"}";
        var signature = signer.sign(body);

        assertThat(signer.sign(body)).isEqualTo(signature);
        assertThat(signer.matches(body, signature)).isTrue();
    }

    @Test
    void rejectsTamperedBodyMissingOrWrongSignature() {
        var body = "{\"amountCents\":4500}";
        var signature = signer.sign(body);

        assertThat(signer.matches("{\"amountCents\":9999}", signature)).isFalse();
        assertThat(signer.matches(body, "deadbeef")).isFalse();
        assertThat(signer.matches(body, null)).isFalse();
        assertThat(signer.matches(body, "")).isFalse();
    }
}
