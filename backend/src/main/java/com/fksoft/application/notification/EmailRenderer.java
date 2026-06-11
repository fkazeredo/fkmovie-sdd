package com.fksoft.application.notification;

import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Renders an outbox row into a sendable {@link EmailMessage}: the HTML and plain-text
 * Thymeleaf templates plus the i18n subject, all resolved in the row's locale (SPEC-0006).
 */
@Component
class EmailRenderer {

    private final TemplateEngine templateEngine;
    private final MessageSource messageSource;

    EmailRenderer(@Qualifier("emailTemplateEngine") TemplateEngine templateEngine, MessageSource messageSource) {
        this.templateEngine = templateEngine;
        this.messageSource = messageSource;
    }

    EmailMessage render(OutboxEmail email) {
        var locale = Locale.forLanguageTag(email.locale());
        var context = new Context(locale);
        for (Map.Entry<String, String> entry : email.payload().entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }
        var template = email.templateKey().templateName();
        var subject = messageSource.getMessage(email.templateKey().subjectKey(), null, locale);
        var html = templateEngine.process(template + ".html", context);
        var text = templateEngine.process(template + ".txt", context);
        return new EmailMessage(email.recipientEmail(), subject, html, text);
    }
}
