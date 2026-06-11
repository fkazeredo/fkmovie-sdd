package com.fksoft.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Executable architecture rules from {@code architecture/}. These tests are authoritative:
 * Claude Code and humans MUST NOT weaken or delete them to make code pass. Propose an ADR
 * to change a rule.
 *
 * <p>Adjust the root package below to your project.
 */
@AnalyzeClasses(packages = "com.fksoft")
class ArchitectureTest {

  /** Controllers must not access repositories directly (architecture/backend.md). */
  @ArchTest
  static final ArchRule controllers_must_not_access_repositories =
      noClasses()
          .that()
          .resideInAPackage("..api..")
          .should()
          .dependOnClassesThat()
          .haveSimpleNameEndingWith("Repository");

  /** Services and domain code must not depend on the web delivery layer. */
  @ArchTest
  static final ArchRule core_must_not_depend_on_api_layer =
      noClasses()
          .that()
          .resideOutsideOfPackages("..api..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..api..");

  /** Module/domain core must not depend on its adapters (queue, infra). */
  @ArchTest
  static final ArchRule core_must_not_depend_on_adapters =
      noClasses()
          .that()
          .resideInAPackage("..application..")
          .and()
          .resideOutsideOfPackages("..api..", "..queue..", "..infra..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..queue..", "..infra..");

  /** Domain/application code must not depend on global infrastructure internals. */
  @ArchTest
  static final ArchRule application_must_not_depend_on_global_infra =
      noClasses()
          .that()
          .resideInAPackage("..application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.fksoft.infra..");

  /** No {@code @Data} on JPA entities — protect invariants (architecture/backend.md). */
  @ArchTest
  static final ArchRule no_lombok_data_on_entities =
      noClasses()
          .that()
          .areAnnotatedWith("jakarta.persistence.Entity")
          .should()
          .beAnnotatedWith("lombok.Data");

  /** Avoid {@code ServiceImpl}-style naming (architecture/backend.md). */
  @ArchTest
  static final ArchRule no_impl_suffix =
      noClasses().should().haveSimpleNameEndingWith("Impl");

  /** Constructor injection only — no field injection. */
  @ArchTest
  static final ArchRule no_field_injection =
      fields()
          .should()
          .notBeAnnotatedWith("org.springframework.beans.factory.annotation.Autowired");

  /** Business exceptions must be specific and meaningful, in business language. */
  @ArchTest
  static final ArchRule exceptions_live_with_their_domain =
      classes()
          .that()
          .haveSimpleNameEndingWith("Exception")
          .and()
          .resideInAPackage("..application..")
          .should()
          .resideOutsideOfPackages("..api..", "..infra..");
}
