import { Injectable, computed, signal } from '@angular/core';

import { AuthUser } from './auth.model';

/**
 * In-memory holder for the access token and current user (SPEC-0021). The access token is
 * deliberately NOT persisted to storage — it lives only in this signal for the page's lifetime;
 * the refresh token is an httpOnly cookie the browser manages, and the session is restored on
 * bootstrap via a silent refresh. This keeps the token out of reach of XSS-readable storage.
 */
@Injectable({ providedIn: 'root' })
export class TokenStore {
  private readonly _accessToken = signal<string | null>(null);
  private readonly _user = signal<AuthUser | null>(null);

  readonly accessToken = this._accessToken.asReadonly();
  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._user() !== null);

  set(accessToken: string, user: AuthUser): void {
    this._accessToken.set(accessToken);
    this._user.set(user);
  }

  patchUser(user: AuthUser): void {
    this._user.set(user);
  }

  clear(): void {
    this._accessToken.set(null);
    this._user.set(null);
  }
}
