package com.fksoft.infra.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Payment gateway settings (ADR 0006): HMAC-SHA256 secret used to validate webhook
 * signatures. Consumed by the payment module from SPEC-0015; validated at startup so the
 * application fails fast when configuration is missing (SPEC-0001).
 */
@Validated
@ConfigurationProperties("app.payment")
public record PaymentProperties(@NotBlank String webhookSecret) {}
