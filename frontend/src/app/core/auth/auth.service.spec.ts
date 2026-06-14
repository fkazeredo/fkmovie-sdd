import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthTokens } from './auth.model';
import { AuthService } from './auth.service';

function tokens(accessToken = 'tok-1', role: 'CUSTOMER' | 'ADMIN' = 'CUSTOMER'): AuthTokens {
  return {
    accessToken,
    accessTokenExpiresAt: '2026-06-20T20:00:00Z',
    user: { id: 'u1', email: 'ana@example.com', name: 'Ana', role, emailVerified: false },
  };
}

const unauthorized = { status: 401, statusText: 'Unauthorized' };

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('logs in and stores the token and user', () => {
    service.login('ana@example.com', 'secret1').subscribe();
    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'ana@example.com', password: 'secret1' });
    req.flush(tokens());

    expect(service.isAuthenticated()).toBe(true);
    expect(service.accessToken()).toBe('tok-1');
    expect(service.role()).toBe('CUSTOMER');
    expect(service.isStaff()).toBe(false);
  });

  it('flags staff and admin from the role', () => {
    service.login('a@b.com', 'secret1').subscribe();
    http.expectOne('/api/auth/login').flush(tokens('tok-1', 'ADMIN'));
    expect(service.isStaff()).toBe(true);
    expect(service.isAdmin()).toBe(true);
  });

  it('registers and auto-logs in', () => {
    service.register('Ana', 'ana@example.com', 'secret1').subscribe();
    const req = http.expectOne('/api/users/register');
    expect(req.request.body).toEqual({
      name: 'Ana',
      email: 'ana@example.com',
      password: 'secret1',
    });
    req.flush(tokens());
    expect(service.isAuthenticated()).toBe(true);
  });

  it('clears the session on logout', () => {
    service.login('a@b.com', 'secret1').subscribe();
    http.expectOne('/api/auth/login').flush(tokens());

    service.logout().subscribe();
    http.expectOne('/api/auth/logout').flush(null);

    expect(service.isAuthenticated()).toBe(false);
    expect(service.accessToken()).toBeNull();
  });

  it('clears the session even when logout fails server-side', () => {
    service.login('a@b.com', 'secret1').subscribe();
    http.expectOne('/api/auth/login').flush(tokens());

    service.logout().subscribe();
    http
      .expectOne('/api/auth/logout')
      .flush({ code: 'x', message: 'x' }, { status: 500, statusText: 'Error' });

    expect(service.isAuthenticated()).toBe(false);
  });

  it('restores the session on bootstrap via the refresh cookie', () => {
    let restored: boolean | undefined;
    service.bootstrap().subscribe((ok) => (restored = ok));
    http.expectOne('/api/auth/refresh').flush(tokens());
    expect(restored).toBe(true);
    expect(service.isAuthenticated()).toBe(true);
  });

  it('stays anonymous when bootstrap refresh fails', () => {
    let restored: boolean | undefined;
    service.bootstrap().subscribe((ok) => (restored = ok));
    http
      .expectOne('/api/auth/refresh')
      .flush({ code: 'auth.invalid-refresh', message: 'x' }, unauthorized);
    expect(restored).toBe(false);
    expect(service.isAuthenticated()).toBe(false);
  });

  it('shares a single in-flight refresh between concurrent callers', () => {
    service.refresh().subscribe();
    service.refresh().subscribe();
    http.expectOne('/api/auth/refresh').flush(tokens());
  });

  it('marks the current user verified after email verification', () => {
    service.login('ana@example.com', 'secret1').subscribe();
    http.expectOne('/api/auth/login').flush(tokens());
    expect(service.emailVerified()).toBe(false);

    service.verifyEmail('the-token').subscribe();
    http
      .expectOne('/api/users/verify-email')
      .flush({ email: 'ana@example.com', emailVerified: true });

    expect(service.emailVerified()).toBe(true);
  });
});
