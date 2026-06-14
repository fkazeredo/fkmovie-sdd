import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

import { AuthService } from './auth.service';

/** True for the auth endpoints that must never trigger a refresh-retry (avoids a refresh loop). */
function isAuthEndpoint(url: string): boolean {
  return (
    url.includes('/api/auth/login') ||
    url.includes('/api/auth/refresh') ||
    url.includes('/api/auth/logout')
  );
}

/**
 * Attaches the Bearer access token to API requests and recovers from an expired token (SPEC-0021):
 * on a 401 (outside the auth endpoints) it performs ONE shared refresh and retries the original
 * request; if the refresh fails the session is cleared and the user is sent to /login with a
 * returnUrl. The error reaching this interceptor is the normalized {@code ApiError} (see the
 * apiError interceptor registered after this one), whose {@code status} field carries the code.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const skipAuth = isAuthEndpoint(request.url);
  const token = auth.accessToken();
  const outgoing =
    token && !skipAuth
      ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : request;

  return next(outgoing).pipe(
    catchError((error: unknown) => {
      const status = (error as { status?: number } | null)?.status;
      if (status !== 401 || skipAuth) {
        return throwError(() => error);
      }
      return auth.refresh().pipe(
        switchMap((restored) => {
          if (!restored) {
            void router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
            return throwError(() => error);
          }
          const retried = request.clone({
            setHeaders: { Authorization: `Bearer ${auth.accessToken()}` },
          });
          return next(retried);
        }),
      );
    }),
  );
};
