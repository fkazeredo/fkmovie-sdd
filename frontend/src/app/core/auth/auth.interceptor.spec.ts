import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';

import { AuthTokens } from './auth.model';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

function tokens(accessToken: string): AuthTokens {
  return {
    accessToken,
    accessTokenExpiresAt: '2026-06-20T20:00:00Z',
    user: { id: 'u1', email: 'a@b.com', name: 'Ana', role: 'CUSTOMER', emailVerified: true },
  };
}

const unauthorized = { status: 401, statusText: 'Unauthorized' };

describe('authInterceptor', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;
  let auth: AuthService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
  });

  afterEach(() => ctrl.verify());

  function seedToken(token = 'tok-1'): void {
    auth.login('a@b.com', 'pw').subscribe();
    ctrl.expectOne('/api/auth/login').flush(tokens(token));
  }

  it('attaches the Bearer token to API requests', () => {
    seedToken('tok-1');
    http.get('/api/data').subscribe();
    const req = ctrl.expectOne('/api/data');
    expect(req.request.headers.get('Authorization')).toBe('Bearer tok-1');
    req.flush({});
  });

  it('does not attach a token when anonymous', () => {
    http.get('/api/data').subscribe();
    const req = ctrl.expectOne('/api/data');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('on 401 performs a single refresh and retries with the new token', () => {
    seedToken('tok-1');
    let result: unknown;
    http.get('/api/data').subscribe((r) => (result = r));

    const first = ctrl.expectOne('/api/data');
    expect(first.request.headers.get('Authorization')).toBe('Bearer tok-1');
    first.flush({ code: 'auth.unauthenticated', message: 'x' }, unauthorized);

    ctrl.expectOne('/api/auth/refresh').flush(tokens('tok-2'));

    const retry = ctrl.expectOne('/api/data');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer tok-2');
    retry.flush({ ok: true });

    expect(result).toEqual({ ok: true });
  });

  it('redirects to /login and clears the session when refresh fails', () => {
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    seedToken('tok-1');
    let errored = false;
    http.get('/api/data').subscribe({ error: () => (errored = true) });

    ctrl.expectOne('/api/data').flush({ code: 'auth.unauthenticated', message: 'x' }, unauthorized);
    ctrl
      .expectOne('/api/auth/refresh')
      .flush({ code: 'auth.invalid-refresh', message: 'x' }, unauthorized);

    expect(errored).toBe(true);
    expect(auth.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(
      ['/login'],
      expect.objectContaining({ queryParams: expect.anything() }),
    );
  });

  it('never triggers a refresh loop on /api/auth/* 401s', () => {
    auth.login('a@b.com', 'pw').subscribe({ error: () => undefined });
    ctrl
      .expectOne('/api/auth/login')
      .flush({ code: 'auth.invalid-credentials', message: 'x' }, unauthorized);
    ctrl.expectNone('/api/auth/refresh');
  });
});
