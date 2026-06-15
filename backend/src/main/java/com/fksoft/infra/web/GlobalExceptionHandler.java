package com.fksoft.infra.web;

import com.fksoft.shared.error.DomainException;
import com.fksoft.shared.error.ErrorDetails;
import com.fksoft.shared.error.RateLimited;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global error handling for every REST endpoint (architecture/backend.md, ADR 0011): translates
 * exceptions into the standard {@link ApiErrorResponse} payload with i18n-resolved messages. Domain
 * exceptions are transport-agnostic — this presentation layer owns the HTTP status ({@link
 * HttpErrorMapping}), the {@code Retry-After} header ({@link RateLimited}) and the response {@code
 * fields} ({@link ErrorDetails}). Unhandled exceptions become {@code internal.error}, logged at ERROR.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /** Domain errors: status from the registry; optional Retry-After / fields from domain data. */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomain(DomainException exception) {
        var body = new ApiErrorResponse(
                exception.code(),
                messageSource.getMessage(exception.code(), exception.args(), LocaleContextHolder.getLocale()),
                fieldsOf(exception));
        var response = ResponseEntity.status(HttpErrorMapping.statusOf(exception.getClass()));
        if (exception instanceof RateLimited rateLimited) {
            response.header(
                    HttpHeaders.RETRY_AFTER,
                    String.valueOf(rateLimited.retryAfter().toSeconds()));
        }
        return response.body(body);
    }

    private static List<ApiErrorResponse.FieldViolation> fieldsOf(DomainException exception) {
        if (exception instanceof ErrorDetails details) {
            return details.details().stream()
                    .map(detail -> new ApiErrorResponse.FieldViolation(detail.key(), detail.value()))
                    .toList();
        }
        return List.of();
    }

    /** Bean Validation failures on request bodies: 400 with one entry per invalid field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException exception) {
        List<ApiErrorResponse.FieldViolation> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiErrorResponse.FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        return new ApiErrorResponse("validation.error", resolveMessage("validation.error"), fields);
    }

    /** Malformed query/path parameter (bad enum, UUID, number, date): 400, not a framework 500. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        var field = new ApiErrorResponse.FieldViolation(exception.getName(), "Invalid value");
        return new ApiErrorResponse("validation.error", resolveMessage("validation.error"), List.of(field));
    }

    /**
     * Concurrency conflict (optimistic {@code @Version} clash on a contended transition): 409, a clear
     * error instead of a raw 500. The caller may retry; the second writer lost the race.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleConcurrentConflict(OptimisticLockingFailureException exception) {
        log.warn("Optimistic lock conflict: {}", exception.getMessage());
        return ApiErrorResponse.of("concurrent.conflict", resolveMessage("concurrent.conflict"));
    }

    /** Requests to unknown paths: 404 in the standard error shape instead of a framework page. */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFound(NoResourceFoundException exception) {
        return ApiErrorResponse.of("not-found", resolveMessage("not-found"));
    }

    /** Known path with unsupported HTTP method: 405 in the standard error shape. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiErrorResponse handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return ApiErrorResponse.of("method.not-allowed", resolveMessage("method.not-allowed"));
    }

    /** Catch-all: 500 {@code internal.error}; the exception is logged, never exposed. */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        return ApiErrorResponse.of("internal.error", resolveMessage("internal.error"));
    }

    private String resolveMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }
}
