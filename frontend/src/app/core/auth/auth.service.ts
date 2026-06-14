import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, tap } from 'rxjs/operators';

import { AuthTokens, AuthUser, Role, VerifyEmailResult } from './auth.model';
import { TokenStore } from './token.store';

/**
 * Authentication state and flows (SPEC-0021, backend SPEC-0003/0004). Holds the session through
 * {@link TokenStore} (access token in memory) and exposes login/register/verify/logout plus the
 * silent-refresh logic that restores the session on bootstrap and recovers from a 401. The refresh
 * token is an httpOnly cookie scoped to {@code /api/auth} that the browser sends automatically.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly store = inject(TokenStore);

  /** Shared in-flight refresh so concurrent 401s trigger a single network call. */
  private refresh$?: Observable<boolean>;

  readonly user = this.store.user;
  readonly accessToken = this.store.accessToken;
  readonly isAuthenticated = this.store.isAuthenticated;
  readonly role = computed<Role | null>(() => this.store.user()?.role ?? null);
  readonly isStaff = computed(() => {
    const role = this.role();
    return role === 'OPERATOR' || role === 'ADMIN';
  });
  readonly isAdmin = computed(() => this.role() === 'ADMIN');
  readonly emailVerified = computed(() => this.store.user()?.emailVerified ?? false);

  login(email: string, password: string): Observable<AuthUser> {
    return this.http.post<AuthTokens>('/api/auth/login', { email, password }).pipe(
      tap((tokens) => this.apply(tokens)),
      map((tokens) => tokens.user),
    );
  }

  register(name: string, email: string, password: string): Observable<AuthUser> {
    return this.http.post<AuthTokens>('/api/users/register', { name, email, password }).pipe(
      tap((tokens) => this.apply(tokens)),
      map((tokens) => tokens.user),
    );
  }

  verifyEmail(token: string): Observable<VerifyEmailResult> {
    return this.http.post<VerifyEmailResult>('/api/users/verify-email', { token }).pipe(
      tap((result) => {
        const current = this.store.user();
        if (current && current.email === result.email) {
          this.store.patchUser({ ...current, emailVerified: result.emailVerified });
        }
      }),
    );
  }

  resendVerification(email: string): Observable<void> {
    return this.http.post<void>('/api/users/resend-verification', { email });
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', {}).pipe(
      catchError(() => of(void 0)),
      finalize(() => this.store.clear()),
    );
  }

  /**
   * Performs a single shared refresh. Concurrent callers share the same request; resolves to
   * {@code true} when the session was restored, {@code false} (and cleared) otherwise.
   */
  refresh(): Observable<boolean> {
    if (!this.refresh$) {
      this.refresh$ = this.http.post<AuthTokens>('/api/auth/refresh', {}).pipe(
        tap((tokens) => this.apply(tokens)),
        map(() => true),
        catchError(() => {
          this.store.clear();
          return of(false);
        }),
        finalize(() => (this.refresh$ = undefined)),
        shareReplay(1),
      );
    }
    return this.refresh$;
  }

  /** Restores the session on app bootstrap via the refresh cookie (never throws). */
  bootstrap(): Observable<boolean> {
    return this.refresh();
  }

  private apply(tokens: AuthTokens): void {
    this.store.set(tokens.accessToken, tokens.user);
  }
}
