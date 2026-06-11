package com.fksoft.architecture;

import com.fksoft.FkmoviesApplication;
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
 * <p>The module set is empty until SPEC-0007: business modules will live under
 * {@code com.fksoft.application.<module>}, and the module detection strategy must be
 * revisited when the first one lands.
 */
class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(FkmoviesApplication.class);

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
