import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';

import { Role } from './auth.model';
import { authGuard, roleGuard } from './auth.guards';
import { AuthService } from './auth.service';

function configure(authenticated: boolean, role: Role | null): void {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([]),
      {
        provide: AuthService,
        useValue: { isAuthenticated: () => authenticated, role: () => role },
      },
    ],
  });
}

const state = { url: '/protected' } as RouterStateSnapshot;
const route = {} as ActivatedRouteSnapshot;

describe('authGuard', () => {
  it('allows an authenticated user', () => {
    configure(true, 'CUSTOMER');
    const result = TestBed.runInInjectionContext(() => authGuard(route, state));
    expect(result).toBe(true);
  });

  it('redirects an anonymous user to /login with the returnUrl', () => {
    configure(false, null);
    const result = TestBed.runInInjectionContext(() => authGuard(route, state)) as UrlTree;
    expect(result).toBeInstanceOf(UrlTree);
    expect(result.toString()).toContain('/login');
    expect(result.toString()).toContain('returnUrl');
  });
});

describe('roleGuard', () => {
  it('allows a matching role', () => {
    configure(true, 'ADMIN');
    const result = TestBed.runInInjectionContext(() => roleGuard('ADMIN')(route, state));
    expect(result).toBe(true);
  });

  it('sends an authenticated user lacking the role home', () => {
    configure(true, 'CUSTOMER');
    const result = TestBed.runInInjectionContext(() => roleGuard('ADMIN')(route, state)) as UrlTree;
    expect(result).toBeInstanceOf(UrlTree);
    expect(result.toString()).toBe('/');
  });

  it('redirects an anonymous user to /login', () => {
    configure(false, null);
    const result = TestBed.runInInjectionContext(() =>
      roleGuard('OPERATOR', 'ADMIN')(route, state),
    ) as UrlTree;
    expect(result.toString()).toContain('/login');
  });
});
