package com.fksoft.shared.error;

import java.util.List;

/**
 * Standard API error payload (architecture/backend.md): a stable machine-readable code, a
 * localized human-readable message and optional per-field violations. Every endpoint error
 * goes through this shape via {@link GlobalExceptionHandler}.
 */
public record ApiErrorResponse(String code, String message, List<FieldViolation> fields) {

    /** Single invalid field with its localized validation message. */
    public record FieldViolation(String field, String message) {}

    /** Creates an error response without field violations. */
    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(code, message, List.of());
    }
}
