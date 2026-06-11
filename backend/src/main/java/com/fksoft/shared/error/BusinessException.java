package com.fksoft.shared.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Base class for business errors (architecture/backend.md): each concrete exception carries
 * a stable machine-readable code resolved to a localized message by the
 * {@link GlobalExceptionHandler}, never a hardcoded user-facing text. Subclasses may add
 * HTTP headers to the response (e.g. {@code Retry-After} on rate limits).
 */
public abstract class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final transient Object[] messageArgs;

    protected BusinessException(HttpStatus status, String code, Object... messageArgs) {
        super(code);
        this.status = status;
        this.code = code;
        this.messageArgs = messageArgs;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public Object[] messageArgs() {
        return messageArgs;
    }

    /** Extra HTTP headers for the error response; empty by default. */
    public Map<String, String> httpHeaders() {
        return Map.of();
    }
}
