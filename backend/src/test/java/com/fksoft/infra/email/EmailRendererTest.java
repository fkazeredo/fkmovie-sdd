package com.fksoft.infra.email;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.domain.notification.EmailTemplate;
import com.fksoft.domain.notification.OutboxEmail;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/** Renders the real templates with the real message bundle in both locales (SPEC-0006). */
class EmailRendererTest {

    private static final ResourceBundleMessageSource MESSAGES = messageSource();
    private final EmailRenderer renderer = new EmailRenderer(engine(), MESSAGES);

    private OutboxEmail verification(String locale) {
        return new OutboxEmail(
                EmailTemplate.VERIFICATION,
                "to@test.local",
                locale,
                Map.of("name", "Frank", "link", "http://localhost:4200/verify-email?token=abc"),
                java.time.Instant.EPOCH);
    }

    @Test
    void rendersPortugueseSubjectAndBodies() {
        var message = renderer.render(verification("pt-BR"));
        assertThat(message.subject()).isEqualTo("Confirme seu e-mail no fkmovies");
        assertThat(message.htmlBody()).contains("Olá Frank").contains("token=abc");
        assertThat(message.textBody()).contains("Olá Frank").contains("token=abc");
    }

    @Test
    void rendersEnglishSubjectAndBodies() {
        var message = renderer.render(verification("en"));
        assertThat(message.subject()).isEqualTo("Confirm your fkmovies email");
        assertThat(message.htmlBody()).contains("Hello Frank");
    }

    private OutboxEmail cancellation(String locale, boolean refund) {
        return new OutboxEmail(
                EmailTemplate.CANCELLATION,
                "to@test.local",
                locale,
                Map.of("name", "Frank", "refundRequested", String.valueOf(refund), "refundAmount", "45.00"),
                java.time.Instant.EPOCH);
    }

    @Test
    void rendersCancellationWithRefundLine() {
        var message = renderer.render(cancellation("pt-BR", true));
        assertThat(message.subject()).isEqualTo("Sua reserva fkmovies foi cancelada");
        assertThat(message.textBody()).contains("Olá Frank").contains("R$ 45.00");
    }

    @Test
    void rendersCancellationWithoutRefundOmitsRefundLine() {
        var message = renderer.render(cancellation("en", false));
        assertThat(message.subject()).isEqualTo("Your fkmovies reservation was cancelled");
        assertThat(message.textBody()).contains("Hello Frank").doesNotContain("refund of R$");
    }

    private static SpringTemplateEngine engine() {
        var engine = new SpringTemplateEngine();
        engine.addTemplateResolver(resolver("*.html", TemplateMode.HTML));
        engine.addTemplateResolver(resolver("*.txt", TemplateMode.TEXT));
        engine.setTemplateEngineMessageSource(MESSAGES);
        return engine;
    }

    private static ClassLoaderTemplateResolver resolver(String pattern, TemplateMode mode) {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/email/");
        resolver.setSuffix("");
        resolver.setTemplateMode(mode);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setResolvablePatterns(Set.of(pattern));
        return resolver;
    }

    private static ResourceBundleMessageSource messageSource() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
