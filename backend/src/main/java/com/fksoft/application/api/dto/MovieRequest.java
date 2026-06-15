package com.fksoft.application.api.dto;

import com.fksoft.domain.screening.AgeRating;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

/**
 * Create/update payload for a movie (SPEC-0008). Both POST and PUT take the same descriptive
 * fields, so a single request type backs both. Bounds mirror the DB constraints.
 */
public record MovieRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull @Min(1) @Max(600) Integer durationMinutes,
        @NotNull AgeRating ageRating,
        @Size(max = 2000) String synopsis,
        @URL @Size(max = 2048) String posterUrl) {}
