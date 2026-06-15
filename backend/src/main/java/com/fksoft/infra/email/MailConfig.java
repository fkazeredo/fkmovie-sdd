package com.fksoft.infra.email;

import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Builds the {@link JavaMailSender} from {@link MailProperties} (ADR 0007): the same Spring
 * Mail implementation everywhere; only the env-driven configuration changes per environment.
 * STARTTLS is enabled for the Gmail SMTP bridge used in dev.
 */
@Configuration
class MailConfig {

    @Bean
    JavaMailSender javaMailSender(MailProperties properties) {
        var sender = new JavaMailSenderImpl();
        sender.setHost(properties.host());
        sender.setPort(properties.port());
        if (!properties.username().isBlank()) {
            sender.setUsername(properties.username());
        }
        if (!properties.password().isBlank()) {
            sender.setPassword(properties.password());
        }
        sender.setDefaultEncoding("UTF-8");
        Properties mailProperties = sender.getJavaMailProperties();
        mailProperties.put("mail.transport.protocol", "smtp");
        mailProperties.put(
                "mail.smtp.auth", String.valueOf(!properties.username().isBlank()));
        mailProperties.put("mail.smtp.starttls.enable", "true");
        return sender;
    }
}
