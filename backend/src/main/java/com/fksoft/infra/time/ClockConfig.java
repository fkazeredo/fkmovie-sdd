package com.fksoft.infra.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single application clock so time-sensitive logic (token TTLs, outbox backoff) is testable
 * by injecting a fixed clock instead of reading {@code Instant.now()} directly.
 */
@Configuration
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
