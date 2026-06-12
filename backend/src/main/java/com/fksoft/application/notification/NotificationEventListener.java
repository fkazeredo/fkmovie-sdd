package com.fksoft.application.notification;

import com.fksoft.application.auth.CustomerRegistered;
import com.fksoft.application.auth.PasswordResetRequested;
import com.fksoft.application.auth.UserInvited;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Bridges auth domain events to outbox emails (SPEC-0006). Reacts AFTER_COMMIT so the email
 * is only enqueued once the source transaction is durable; the actual send is the
 * dispatcher's job.
 */
@Component
class NotificationEventListener {

    private final EmailOutbox outbox;
    private final String baseUrl;

    NotificationEventListener(EmailOutbox outbox, @Value("${app.notification.base-url}") String baseUrl) {
        this.outbox = outbox;
        this.baseUrl = baseUrl;
    }

    @TransactionalEventListener
    void on(CustomerRegistered event) {
        var link = link("/verify-email", event.verificationToken());
        outbox.enqueue(
                EmailTemplate.VERIFICATION,
                event.email(),
                event.preferredLocale(),
                Map.of("name", event.name(), "link", link));
    }

    @TransactionalEventListener
    void on(PasswordResetRequested event) {
        var link = link("/reset-password", event.resetToken());
        outbox.enqueue(
                EmailTemplate.PASSWORD_RESET,
                event.email(),
                event.preferredLocale(),
                Map.of("name", event.name(), "link", link));
    }

    @TransactionalEventListener
    void on(UserInvited event) {
        var link = link("/accept-invitation", event.invitationToken());
        outbox.enqueue(
                EmailTemplate.INVITATION,
                event.email(),
                event.preferredLocale(),
                Map.of("name", event.name(), "link", link, "role", event.role().name()));
    }

    private String link(String path, String token) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path(path)
                .queryParam("token", token)
                .build()
                .toUriString();
    }
}
