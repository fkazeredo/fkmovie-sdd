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

    /** Services and domain code must not depend on the web delivery layer. */
    @Test
    void coreMustNotDependOnApiLayer() {
        noClasses()
                .that()
                .resideOutsideOfPackages("..api..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..api..")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    /** Module/domain core must not depend on its adapters (queue, infra). */
    @Test
    void coreMustNotDependOnAdapters() {
        noClasses()
                .that()
                .resideInAPackage("..application..")
                .and()
                .resideOutsideOfPackages("..api..", "..queue..", "..infra..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..queue..", "..infra..")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    /** Domain/application code must not depend on global infrastructure internals. */
    @Test
    void applicationMustNotDependOnGlobalInfra() {
        noClasses()
                .that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.fksoft.infra..")
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
        var authPersistence = JavaClass.Predicates.resideInAPackage("com.fksoft.application.auth..")
                .and(JavaClass.Predicates.simpleNameEndingWith("Repository")
                        .or(CanBeAnnotated.Predicates.annotatedWith("jakarta.persistence.Entity")));
        noClasses()
                .that()
                .resideOutsideOfPackage("com.fksoft.application.auth..")
                .should()
                .dependOnClassesThat(authPersistence)
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    /**
     * SPEC-0006: the notification module's outbox is module-internal — other modules trigger
     * email by publishing events, never by touching {@code OutboxEmail}/its repository.
     */
    @Test
    void otherModulesMustNotTouchNotificationPersistence() {
        var notificationPersistence = JavaClass.Predicates.resideInAPackage("com.fksoft.application.notification..")
                .and(JavaClass.Predicates.simpleNameEndingWith("Repository")
                        .or(CanBeAnnotated.Predicates.annotatedWith("jakarta.persistence.Entity")));
        noClasses()
                .that()
                .resideOutsideOfPackage("com.fksoft.application.notification..")
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
        var cinemaPersistence = JavaClass.Predicates.resideInAPackage("com.fksoft.application.cinema..")
                .and(JavaClass.Predicates.simpleNameEndingWith("Repository")
                        .or(CanBeAnnotated.Predicates.annotatedWith("jakarta.persistence.Entity")));
        noClasses()
                .that()
                .resideOutsideOfPackage("com.fksoft.application.cinema..")
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
        var screeningPersistence = JavaClass.Predicates.resideInAPackage("com.fksoft.application.screening..")
                .and(JavaClass.Predicates.simpleNameEndingWith("Repository")
                        .or(CanBeAnnotated.Predicates.annotatedWith("jakarta.persistence.Entity")));
        noClasses()
                .that()
                .resideOutsideOfPackage("com.fksoft.application.screening..")
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
        var bookingPersistence = JavaClass.Predicates.resideInAPackage("com.fksoft.application.booking..")
                .and(JavaClass.Predicates.simpleNameEndingWith("Repository")
                        .or(CanBeAnnotated.Predicates.annotatedWith("jakarta.persistence.Entity")));
        noClasses()
                .that()
                .resideOutsideOfPackage("com.fksoft.application.booking..")
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
        var pricingPersistence = JavaClass.Predicates.resideInAPackage("com.fksoft.application.pricing..")
                .and(JavaClass.Predicates.simpleNameEndingWith("Repository")
                        .or(CanBeAnnotated.Predicates.annotatedWith("jakarta.persistence.Entity")));
        noClasses()
                .that()
                .resideOutsideOfPackage("com.fksoft.application.pricing..")
                .should()
                .dependOnClassesThat(pricingPersistence)
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    /** Business exceptions must be specific and meaningful, in business language. */
    @Test
    void exceptionsLiveWithTheirDomain() {
        classes()
                .that()
                .haveSimpleNameEndingWith("Exception")
                .and()
                .resideInAPackage("..application..")
                .should()
                .resideOutsideOfPackages("..api..", "..infra..")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }
}
