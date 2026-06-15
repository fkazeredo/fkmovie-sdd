package com.fksoft.infra.i18n;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Internationalization message source (centralized infra). Defining it here gives i18n one
 * obvious home; it binds {@code spring.messages.*} faithfully — notably
 * {@code fallback-to-system-locale=false} (SPEC-0006: an English user never falls back to the
 * server's Portuguese bundle). Bundles live in {@code src/main/resources/messages*.properties};
 * the request locale comes from {@code Accept-Language} (Spring's default resolver).
 */
@Configuration
@EnableConfigurationProperties(MessageSourceProperties.class)
class MessageSourceConfig {

    @Bean
    MessageSource messageSource(MessageSourceProperties properties) {
        var messageSource = new ResourceBundleMessageSource();
        List<String> basenames = properties.getBasename();
        if (basenames != null && !basenames.isEmpty()) {
            messageSource.setBasenames(basenames.toArray(String[]::new));
        }
        if (properties.getEncoding() != null) {
            messageSource.setDefaultEncoding(properties.getEncoding().name());
        }
        messageSource.setFallbackToSystemLocale(properties.isFallbackToSystemLocale());
        Duration cacheDuration = properties.getCacheDuration();
        if (cacheDuration != null) {
            messageSource.setCacheMillis(cacheDuration.toMillis());
        }
        messageSource.setAlwaysUseMessageFormat(properties.isAlwaysUseMessageFormat());
        messageSource.setUseCodeAsDefaultMessage(properties.isUseCodeAsDefaultMessage());
        return messageSource;
    }
}
