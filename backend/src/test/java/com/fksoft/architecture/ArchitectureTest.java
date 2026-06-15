package com.fksoft.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * Executable architecture rules from {@code architecture/}. These tests are authoritative:
 * Claude Code and humans MUST NOT weaken or delete them to make code pass. Propose an ADR
 * to change a rule.
 *
 * <p>Plain JUnit {@code @Test} methods over the ArchUnit core API instead of
 * {@code archunit-junit5}: the ArchUnit JUnit engine does not run on JUnit Platform 6
 * (Spring Boot 4 default) yet — TNG/ArchUnit issue #1556. Every rule allows an empty
 * match set because business module packages only appear from SPEC-0007.
 */
class ArchitectureTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.fksoft");

    /** Controllers must not access repositories directly (architecture/backend.md). */
    @Test
    void controllersMustNotAccessRepositories() {
        noClasses()
                .that()
                .resideInAPackage("..api..")
                .should()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("Repository")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    /**
     * The domain is the pure hexagon core (ADR 0012): it must not depend on the delivery layer
     * ({@code com.fksoft.application}, i.e. controllers/realtime) nor on infrastructure
     * ({@code com.fksoft.infra}). Delivery and infra may depend on the domain, never the reverse.
     */
    @Test
    void domainMustNotDependOnDeliveryOrInfra() {
        noClasses()
                .that()
                .resideInAPackage("com.fksoft.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("com.fksoft.application..", "com.fksoft.infra..")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    /**
     * Infrastructure is a driven adapter (ADR 0012): it may depend on the domain, but never on the
     * delivery layer ({@code com.fksoft.application}). Keeps the dependency flow delivery → infra,
     * not the reverse.
     */
    @Test
    void infraMustNotDependOnDelivery() {
        noClasses()
                .that()
                .resideInAPackage("com.fksoft.infra..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.fksoft.application..")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    /** No {@code @Data} on JPA entities — protect invariants (architecture/backend.md). */
    @Test
    void noLombokDataOnEntities() {
        noClasses()
                .that()
                .areAnnotatedWith("jakarta.persistence.Entity")
                .should()
                .beAnnotatedWith("lombok.Data")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    /**
     * No {@code @Setter} on JPA entities — Lombok is welcome for boilerplate ({@code @Getter},
     * {@code @NoArgsConstructor}), but entities mutate only through meaningful business methods,
     * never uncontrolled setters (architecture/backend.md).
     */
    @Test
    void noLombokSetterOnEntities() {
        noClasses()
                .that()
                .areAnnotatedWith("jakarta.persistence.Entity")
                .should()
                .beAnnotatedWith("lombok.Setter")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    /** Avoid {@code ServiceImpl}-style naming (architecture/backend.md). */
    @Test
    void noImplSuffix() {
        noClasses()
                .should()
                .haveSimpleNameEndingWith("Impl")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    /** Constructor injection only — no field injection. */
    @Test
    void noFieldInjection() {
        fields().should()
                .notBeAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    /**
     * SPEC-0003 architectural test: no other module accesses the auth module's persistence
     * (its {@code users} table) directly — repositories and entities are module-internal.
     */
    @Test
    void otherModulesMustNotTouchAuthPersistence() {
        var authPersistence = JavaClass.Predicates.resideInAPackage("com.fksoft.domain.auth..")
                .and(JavaClass.Predicates.simpleNameEndingWith("Repository")
                        .or(CanBeAnnotated.Predicates.annotatedWith("jakarta.persistence.Entity")));
        noClasses()
                .that()
                .resideOutsideOfPackage("com.fksoft.domain.auth..")
                .should()
                .dependOnClassesThat(authPersistence)
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    /**
     * SPEC-0006 / ADR 0010: the notification module's outbox is internal to the module — other
     * business modules trigger email by publishing events, never by touching {@code OutboxEmail}/
     * its repository. The centralized {@code com.fksoft.infra} layer is exempt: its email adapter
     * (SMTP sender, renderer, outbox dispatch worker) legitimately operates the module's outbox.
     */
    @Test
    void otherModulesMustNotTouchNotificationPersistence() {
        var notificationPersistence = JavaClass.Predicates.resideInAPackage("com.fksoft.domain.notification..")
                .and(JavaClass.Predicates.simpleNameEndingWith("Repository")
                        .or(CanBeAnnotated.Predicates.annotatedWith("jakarta.persistence.Entity")));
        noClasses()
                .that()
                .resideOutsideOfPackages("com.fksoft.domain.notification..", "com.fksoft.infra..")
                .should()
                .dependOnClassesThat(notificationPersistence)
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    /**
     * SPEC-0007: the cinema module's persistence ({@code CinemaRoom}/{@code Seat} and their
     * repositories) is module-internal. The first consumer (SPEC-0011) will read seats through a
     * public module API, never by touching these classes — see
     * {@code architecture/simulation-and-mocking.md}.
     */
    @Test
    void otherModulesMustNotTouchCinemaPersistence() {
        var cinemaPersistence = JavaClass.Predicates.resideInAPackage("com.fksoft.domain.cinema..")
                .and(JavaClass.Predicates.simpleNameEndingWith("Repository")
                        .or(CanBeAnnotated.Predicates.annotatedWith("jakarta.persistence.Entity")));
        noClasses()
                .that()
                .resideOutsideOfPackage("com.fksoft.domain.cinema..")
                .should()
                .dependOnClassesThat(cinemaPersistence)
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    /**
     * SPEC-0008: the screening module's persistence ({@code Movie} and its repository) is
     * module-internal. Other modules collaborate through its public API/events, never by touching
     * these classes.
     */
    @Test
    void otherModulesMustNotTouchScreeningPersistence() {
        var screeningPersistence = JavaClass.Predicates.resideInAPackage("com.fksoft.domain.screening..")
                .and(JavaClass.Predicates.simpleNameEndingWith("Repository")
                        .or(CanBeAnnotated.Predicates.annotatedWith("jakarta.persistence.Entity")));
        noClasses()
                .that()
                .resideOutsideOfPackage("com.fksoft.domain.screening..")
                .should()
                .dependOnClassesThat(screeningPersistence)
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    /**
     * SPEC-0009: the booking module's persistence ({@code ScreeningSeat} and its repository) is
     * module-internal. Other modules collaborate through events and its public API, never these
     * classes.
     */
    @Test
    void otherModulesMustNotTouchBookingPersistence() {
        var bookingPersistence = JavaClass.Predicates.resideInAPackage("com.fksoft.domain.booking..")
                .and(JavaClass.Predicates.simpleNameEndingWith("Repository")
                        .or(CanBeAnnotated.Predicates.annotatedWith("jakarta.persistence.Entity")));
        noClasses()
                .that()
                .resideOutsideOfPackage("com.fksoft.domain.booking..")
                .should()
                .dependOnClassesThat(bookingPersistence)
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    /**
     * SPEC-0012: the pricing module's config persistence ({@code PricingSeatType}/{@code
     * PricingWeekday} and their repositories) is module-internal. Other modules price through the
     * public {@code PriceCalculator} facade, never these classes.
     */
    @Test
    void otherModulesMustNotTouchPricingPersistence() {
        var pricingPersistence = JavaClass.Predicates.resideInAPackage("com.fksoft.domain.pricing..")
                .and(JavaClass.Predicates.simpleNameEndingWith("Repository")
                        .or(CanBeAnnotated.Predicates.annotatedWith("jakarta.persistence.Entity")));
        noClasses()
                .that()
                .resideOutsideOfPackage("com.fksoft.domain.pricing..")
                .should()
                .dependOnClassesThat(pricingPersistence)
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    /**
     * SPEC-0015 / ADR 0010: the payment module's ledger ({@code Payment}/{@code MockPaymentJob}/
     * {@code PaymentWebhookEvent} and their repositories) is internal to the module. Other business
     * modules use the {@code PaymentGateway} port and react to its events, never these classes. The
     * centralized {@code com.fksoft.infra} layer is exempt: its mock-gateway integration adapter
     * legitimately writes the ledger and the mock delivery queue.
     */
    @Test
    void otherModulesMustNotTouchPaymentPersistence() {
        var paymentPersistence = JavaClass.Predicates.resideInAPackage("com.fksoft.domain.payment..")
                .and(JavaClass.Predicates.simpleNameEndingWith("Repository")
                        .or(CanBeAnnotated.Predicates.annotatedWith("jakarta.persistence.Entity")));
        noClasses()
                .that()
                .resideOutsideOfPackages("com.fksoft.domain.payment..", "com.fksoft.infra..")
                .should()
                .dependOnClassesThat(paymentPersistence)
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    /** Business exceptions live in the domain, never in the delivery or infra layers. */
    @Test
    void exceptionsLiveWithTheirDomain() {
        classes()
                .that()
                .haveSimpleNameEndingWith("Exception")
                .and()
                .resideInAPackage("com.fksoft.domain..")
                .should()
                .resideOutsideOfPackages("..api..", "..infra..")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }
}
