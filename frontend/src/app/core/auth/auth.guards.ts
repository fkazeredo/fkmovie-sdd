import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';
import { Role } from './auth.model';

/** Requires any authenticated user; otherwise redirects to /login with the attempted URL. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

/**
 * Requires one of the given roles. Anonymous users go to /login (with returnUrl); authenticated
 * users lacking the role are sent home — they are logged in but not allowed here (SPEC-0021).
 */
export function roleGuard(...roles: Role[]): CanActivateFn {
  return (_route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (!auth.isAuthenticated()) {
      return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    }
    const role = auth.role();
    return role !== null && roles.includes(role) ? true : router.createUrlTree(['/']);
  };
}
