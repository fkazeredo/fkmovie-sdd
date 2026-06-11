package com.fksoft;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * fkmovies modular monolith entry point (ADR 0001). Business modules live under
 * {@code com.fksoft.application.<module>} and are introduced from SPEC-0007 onward.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class FkmoviesApplication {

    /**
     * Boots the application. The JVM default timezone is forced to {@code APP_TIMEZONE}
     * (UTC unless overridden) before the context starts, so time handling never depends on
     * the host timezone (SPEC-0001). The future Dockerfile must also pass
     * {@code -Duser.timezone=UTC} for tools that bypass this entry point.
     */
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(System.getenv().getOrDefault("APP_TIMEZONE", "UTC")));
        SpringApplication.run(FkmoviesApplication.class, args);
    }
}
