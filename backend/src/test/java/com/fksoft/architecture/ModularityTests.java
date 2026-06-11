package com.fksoft.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith boundary verification (architecture/modules-and-apis.md).
 *
 * <p>{@code verify()} fails the build when a module accesses another module's internals,
 * enforcing the cross-module rules deterministically: synchronous collaboration only through
 * public module APIs, no dependency on another module's repositories, entities or
 * implementation classes.
 *
 * <p>Requires the {@code spring-modulith-starter-test} dependency (see
 * backend/config/pom-snippets.xml) and module packages directly under the application root
 * (e.g. {@code com.fksoft.application.booking}). Adjust the main class reference.
 */
class ModularityTests {

  ApplicationModules modules = ApplicationModules.of(com.fksoft.Application.class);

  /** Fails when any module violates declared boundaries or accesses internals. */
  @Test
  void verifiesModularStructure() {
    modules.verify();
  }

  /** Generates module documentation (PlantUML/AsciiDoc) under target/spring-modulith-docs. */
  @Test
  void writeDocumentationSnippets() {
    new Documenter(modules).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
  }
}
