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
 * <p>Modules are detected via {@code spring.modulith.detection-strategy=explicitly-annotated}
 * (application.yaml): every {@code com.fksoft.application.<module>} package carries
 * {@code @ApplicationModule} on its package-info; {@code infra}/{@code shared} are
 * arrangement code governed by ArchUnit, not modules.
 */
class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(FkmoviesApplication.class);

    /** Fails when any module violates declared boundaries or accesses internals. */
    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    /** Guards the detection strategy: if the property were ignored, verify() would pass vacuously. */
    @Test
    void detectsBusinessModules() {
        org.assertj.core.api.Assertions.assertThat(modules.getModuleByName("application.auth"))
                .isPresent();
        org.assertj.core.api.Assertions.assertThat(modules.getModuleByName("application.notification"))
                .isPresent();
        org.assertj.core.api.Assertions.assertThat(modules.getModuleByName("application.cinema"))
                .isPresent();
    }

    /** Generates module documentation (PlantUML/AsciiDoc) under target/spring-modulith-docs. */
    @Test
    void writeDocumentationSnippets() {
        new Documenter(modules).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
    }
}
