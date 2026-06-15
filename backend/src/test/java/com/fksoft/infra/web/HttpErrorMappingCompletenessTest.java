package com.fksoft.infra.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.domain.error.DomainException;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * ADR 0011 guard: every concrete {@link DomainException} subclass MUST have an explicit HTTP status
 * in {@link HttpErrorMapping}. This turns the registry's only weakness (a forgotten entry silently
 * falling back to 422) into a build failure — add an exception, you must map it.
 */
class HttpErrorMappingCompletenessTest {

    @Test
    void everyDomainExceptionHasAnExplicitStatus() {
        JavaClasses production = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.fksoft");
        List<String> mapped =
                HttpErrorMapping.mappedTypes().stream().map(Class::getName).toList();

        List<String> unmapped = production.stream()
                .filter(javaClass -> javaClass.isAssignableTo(DomainException.class))
                .filter(javaClass -> !javaClass.getModifiers().contains(JavaModifier.ABSTRACT))
                .map(JavaClass::getFullName)
                .filter(name -> !mapped.contains(name))
                .toList();

        assertThat(unmapped)
                .as("DomainException subclasses missing an HTTP status in HttpErrorMapping")
                .isEmpty();
    }
}
