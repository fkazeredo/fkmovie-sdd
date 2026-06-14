import { HttpInterceptorFn } from '@angular/common/http';

export const CORRELATION_ID_HEADER = 'X-Correlation-Id';

/**
 * Generates an RFC-4122-ish v4 UUID that works in ANY context. `crypto.randomUUID()` only
 * exists in secure contexts (HTTPS or localhost); over plain HTTP (IP/LAN) it is undefined and
 * throwing here would crash app bootstrap (blank page). Falls back to `getRandomValues` and, as a
 * last resort, `Math.random`. The `cryptoObj` parameter exists only to make the fallback testable.
 */
export function generateCorrelationId(
  cryptoObj: Crypto | undefined = typeof globalThis !== 'undefined' ? globalThis.crypto : undefined,
): string {
  if (cryptoObj && typeof cryptoObj.randomUUID === 'function') {
    return cryptoObj.randomUUID();
  }
  if (cryptoObj && typeof cryptoObj.getRandomValues === 'function') {
    const bytes = cryptoObj.getRandomValues(new Uint8Array(16));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0'));
    return `${hex.slice(0, 4).join('')}-${hex.slice(4, 6).join('')}-${hex.slice(6, 8).join('')}-${hex.slice(8, 10).join('')}-${hex.slice(10, 16).join('')}`;
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (ch) => {
    const r = (Math.random() * 16) | 0;
    return (ch === 'x' ? r : (r & 0x3) | 0x8).toString(16);
  });
}

/** One correlation ID per browser session — matches the backend MDC filter (spec 0001). */
const sessionCorrelationId = generateCorrelationId();

/**
 * Adds the `X-Correlation-Id` header to every outgoing request so backend logs can be
 * traced back to a frontend session (spec 0002, architecture/observability.md).
 */
export const correlationIdInterceptor: HttpInterceptorFn = (request, next) =>
  next(request.clone({ setHeaders: { [CORRELATION_ID_HEADER]: sessionCorrelationId } }));
