import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

import { toApiError } from './api-error.model';

/**
 * Normalizes every failed HTTP response into a typed {@link ApiError} so features
 * never parse the backend error JSON themselves (spec 0002). Presentation of errors
 * is feature-specific and specified from spec 0021 onward.
 */
export const apiErrorInterceptor: HttpInterceptorFn = (request, next) =>
  next(request).pipe(
    catchError((error: unknown) =>
      throwError(() => (error instanceof HttpErrorResponse ? toApiError(error) : error)),
    ),
  );
