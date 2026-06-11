import { HttpErrorResponse } from '@angular/common/http';

/** Single invalid field reported by the backend. */
export interface ApiFieldViolation {
  field: string;
  message: string;
}

/**
 * Normalized API error (spec 0002): the backend's `{ code, message, fields }` payload
 * plus the HTTP status. Errors that carry no backend payload (network failures,
 * non-JSON bodies) are normalized to `network.error`.
 */
export interface ApiError {
  code: string;
  message: string;
  fields: ApiFieldViolation[];
  status: number;
}

export function toApiError(response: HttpErrorResponse): ApiError {
  const body: unknown = response.error;
  if (isBackendErrorBody(body)) {
    return {
      code: body.code,
      message: body.message,
      fields: body.fields ?? [],
      status: response.status,
    };
  }
  return {
    code: 'network.error',
    message: response.message,
    fields: [],
    status: response.status,
  };
}

function isBackendErrorBody(
  body: unknown,
): body is { code: string; message: string; fields?: ApiFieldViolation[] } {
  return (
    typeof body === 'object' &&
    body !== null &&
    typeof (body as Record<string, unknown>)['code'] === 'string' &&
    typeof (body as Record<string, unknown>)['message'] === 'string'
  );
}
