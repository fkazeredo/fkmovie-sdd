package com.fksoft.application.notification;

import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * A Thymeleaf engine dedicated to emails (SPEC-0006): the templates carry their extension so
 * one resolver renders {@code *.html} in HTML mode and another renders {@code *.txt} in TEXT
 * mode (plain-text fallback). Uses {@link SpringTemplateEngine} (SpEL dialect) so it needs no
 * OGNL, and resolves {@code #{...}} message expressions against the application bundle.
 */
@Configuration
class EmailTemplateConfig {

    @Bean
    SpringTemplateEngine emailTemplateEngine(MessageSource messageSource) {
        var engine = new SpringTemplateEngine();
        engine.addTemplateResolver(resolver("*.html", TemplateMode.HTML, 1));
        engine.addTemplateResolver(resolver("*.txt", TemplateMode.TEXT, 2));
        engine.setTemplateEngineMessageSource(messageSource);
        return engine;
    }

    private static ClassLoaderTemplateResolver resolver(String pattern, TemplateMode mode, int order) {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/email/");
        resolver.setSuffix("");
        resolver.setTemplateMode(mode);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setResolvablePatterns(Set.of(pattern));
        resolver.setOrder(order);
        resolver.setCacheable(true);
        return resolver;
    }
}
