package com.fksoft.domain.notification;

import com.fksoft.domain.auth.CustomerRegistered;
import com.fksoft.domain.auth.PasswordResetRequested;
import com.fksoft.domain.auth.UserInvited;
import com.fksoft.domain.booking.ReservationCancellationConfirmed;
import com.fksoft.domain.booking.ReservationConfirmed;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
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

    @TransactionalEventListener
    void on(ReservationConfirmed event) {
        var ticketLines = event.tickets().stream()
                .map(ticket -> ticket.seatLabel() + " — " + ticket.code())
                .collect(Collectors.joining("\n"));
        outbox.enqueue(
                EmailTemplate.TICKETS,
                event.email(),
                event.preferredLocale(),
                Map.of("name", event.name(), "tickets", ticketLines));
    }

    @TransactionalEventListener
    void on(ReservationCancellationConfirmed event) {
        var refundAmount = String.format(Locale.US, "%.2f", event.refundAmountCents() / 100.0);
        outbox.enqueue(
                EmailTemplate.CANCELLATION,
                event.email(),
                event.preferredLocale(),
                Map.of(
                        "name", event.name(),
                        "refundRequested", String.valueOf(event.refundRequested()),
                        "refundAmount", refundAmount));
    }

    private String link(String path, String token) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path(path)
                .queryParam("token", token)
                .build()
                .toUriString();
    }
}
