import { HttpInterceptorFn } from '@angular/common/http';

export const CORRELATION_ID_HEADER = 'X-Correlation-Id';

/** One correlation ID per browser session — matches the backend MDC filter (spec 0001). */
const sessionCorrelationId = crypto.randomUUID();

/**
 * Adds the `X-Correlation-Id` header to every outgoing request so backend logs can be
 * traced back to a frontend session (spec 0002, architecture/observability.md).
 */
export const correlationIdInterceptor: HttpInterceptorFn = (request, next) =>
  next(request.clone({ setHeaders: { [CORRELATION_ID_HEADER]: sessionCorrelationId } }));
