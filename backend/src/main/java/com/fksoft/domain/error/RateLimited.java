package com.fksoft.domain.error;

import java.time.Duration;

/**
 * Domain data for a rate-limited error: how long the caller should wait before retrying. The
 * presentation layer turns this into a {@code Retry-After} header; the domain only states the wait.
 */
public interface RateLimited {

    Duration retryAfter();
}
