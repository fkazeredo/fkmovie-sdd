package com.fksoft.infra.email;

import com.fksoft.domain.notification.EmailMessage;
import com.fksoft.domain.notification.EmailSendException;
import com.fksoft.domain.notification.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Spring Mail implementation of {@link EmailSender} (ADR 0007). Builds a multipart MIME
 * message (HTML + plain-text fallback), honours {@code MAIL_TO_OVERRIDE} by redirecting
 * every recipient to a single address in dev/staging, and classifies send failures so the
 * outbox can decide whether to retry.
 */
@Component
class SpringMailEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String from;
    private final String toOverride;

    SpringMailEmailSender(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.mail.to-override:}") String toOverride) {
        this.mailSender = mailSender;
        this.from = from;
        this.toOverride = toOverride;
    }

    @Override
    public void send(EmailMessage message) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            var helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(effectiveRecipient(message.to()));
            helper.setSubject(message.subject());
            helper.setText(message.textBody(), message.htmlBody());
        } catch (MessagingException e) {
            throw new EmailSendException("Failed to build MIME message", false, e);
        }
        try {
            mailSender.send(mimeMessage);
        } catch (MailParseException | MailAuthenticationException e) {
            throw new EmailSendException("Permanent mail failure", false, e);
        } catch (MailSendException e) {
            throw new EmailSendException("Transient mail failure", true, e);
        }
    }

    private String effectiveRecipient(String recipient) {
        return toOverride == null || toOverride.isBlank() ? recipient : toOverride;
    }
}
