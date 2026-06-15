package com.fksoft.domain.notification;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** Registers the outbox-size gauges (SPEC-0006 observability) per status. */
@Component
class NotificationMetrics {

    NotificationMetrics(OutboxEmailRepository repository, MeterRegistry meterRegistry) {
        for (OutboxStatus status : OutboxStatus.values()) {
            Gauge.builder("email.outbox.size", () -> repository.countByStatus(status))
                    .tag("status", status.name())
                    .register(meterRegistry);
        }
    }
}
