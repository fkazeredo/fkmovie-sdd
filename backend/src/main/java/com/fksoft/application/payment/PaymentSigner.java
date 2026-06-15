package com.fksoft.application.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * HMAC-SHA256 signing/verification for payment webhooks (SPEC-0015, ADR 0006). The mock signs with
 * the same scheme a real gateway will use, so the verification code is provider-agnostic. The
 * comparison is constant-time.
 */
@Component
public class PaymentSigner {

    private final byte[] secret;

    PaymentSigner(@Value("${app.payment.webhook-secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** Returns the hex HMAC-SHA256 signature of the body, using the configured webhook secret. */
    public String sign(String body) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign payment webhook", ex);
        }
    }

    boolean matches(String body, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                sign(body).getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }
}
