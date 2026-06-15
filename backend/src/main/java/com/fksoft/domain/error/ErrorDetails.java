package com.fksoft.domain.error;

import java.util.List;

/**
 * Domain data attached to an error as key/value pairs (e.g. the unavailable seat ids). The
 * presentation layer renders these into the response body's {@code fields}; the domain only
 * exposes the data, never the API shape.
 */
public interface ErrorDetails {

    List<Detail> details();

    record Detail(String key, String value) {}
}
