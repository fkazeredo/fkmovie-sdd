package com.fksoft.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.infra.email.MailProperties;
import com.fksoft.infra.integration.PaymentProperties;
import com.fksoft.infra.security.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Regression for the SPEC-0001 fail-fast rule: startup MUST fail when a required env var is
 * missing. The Boot binder ignores unresolvable {@code ${VAR}} placeholders and binds the
 * literal text, which silently satisfied {@code @NotBlank} — application.yaml now defaults
 * required values to empty so Bean Validation rejects them. Each test loads the real
 * application.yaml (no test profile, no env vars set) and expects context startup to fail.
 */
class RequiredPropertiesFailFastTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer());

    @Test
    void startupFailsWhenJwtEnvVarsAreMissing() {
        runner.withUserConfiguration(JwtOnly.class)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void startupFailsWhenMailEnvVarsAreMissing() {
        runner.withUserConfiguration(MailOnly.class)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void startupFailsWhenPaymentEnvVarsAreMissing() {
        runner.withUserConfiguration(PaymentOnly.class)
                .run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties(JwtProperties.class)
    static class JwtOnly {}

    @EnableConfigurationProperties(MailProperties.class)
    static class MailOnly {}

    @EnableConfigurationProperties(PaymentProperties.class)
    static class PaymentOnly {}
}
