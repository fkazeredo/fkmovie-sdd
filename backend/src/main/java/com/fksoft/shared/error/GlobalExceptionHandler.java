package com.fksoft.shared.error;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global error handling for every REST endpoint (architecture/backend.md): translates
 * exceptions into the standard {@link ApiErrorResponse} payload with i18n-resolved
 * messages. Unhandled exceptions become {@code internal.error} and are logged at ERROR.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /** Business errors: status, stable code and optional headers defined by the exception. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException exception) {
        var body = new ApiErrorResponse(
                exception.code(),
                messageSource.getMessage(exception.code(), exception.messageArgs(), LocaleContextHolder.getLocale()),
                exception.fields());
        var response = ResponseEntity.status(exception.status());
        exception.httpHeaders().forEach(response::header);
        return response.body(body);
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
