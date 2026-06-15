package com.fksoft.infra.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT settings (ADR 0005): signing secret plus access/refresh token lifetimes. Consumed by
 * the authentication module from SPEC-0003; validated at startup so the application fails
 * fast when configuration is missing (SPEC-0001).
 */
@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(@NotBlank String secret, @NotNull Duration accessTtl, @NotNull Duration refreshTtl) {}
