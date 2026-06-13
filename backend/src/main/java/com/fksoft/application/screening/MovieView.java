package com.fksoft.application.screening;

import java.util.UUID;

/**
 * Stable movie projection exposed to other modules (SPEC-0019). Part of the screening module's
 * public read API ({@link MovieCatalog}); never the {@code Movie} entity.
 */
public record MovieView(
        UUID id, String title, AgeRating ageRating, int durationMinutes, String posterUrl, MovieStatus status) {}
