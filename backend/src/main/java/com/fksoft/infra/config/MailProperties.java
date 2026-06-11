package com.fksoft.infra.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Outgoing mail settings (ADR 0007). Consumed by the notification module from SPEC-0006;
 * validated at startup so the application fails fast when configuration is missing
 * (SPEC-0001). Username and password are optional for unauthenticated local SMTP;
 * {@code toOverride}, when set, redirects every outgoing email (non-prod safety).
 */
@Validated
@ConfigurationProperties("app.mail")
public record MailProperties(
        @NotBlank String host,
        @Min(1) int port,
        String username,
        String password,
        @NotBlank String from,
        String toOverride) {}
