package com.fksoft.domain.error;

/**
 * Base for business (domain) errors. Carries only domain data: a stable machine-readable
 * {@code code} (also the i18n message key) and optional message arguments. It has NO transport
 * concern — the presentation layer ({@code com.fksoft.infra.web}) maps the exception type to an
 * HTTP status and renders the response body. Subclasses MAY additionally implement
 * {@link ErrorDetails} or {@link RateLimited} to surface extra domain data to the presentation.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;
    private final transient Object[] args;

    protected DomainException(String code, Object... args) {
        super(code);
        this.code = code;
        this.args = args;
    }

    public String code() {
        return code;
    }

    public Object[] args() {
        return args;
    }
}
