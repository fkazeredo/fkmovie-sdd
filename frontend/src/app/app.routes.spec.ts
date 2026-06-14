import { routes } from './app.routes';

/**
 * Regression: the email verification link is built by the backend
 * (NotificationEventListener → `${APP_BASE_URL}/verify-email?token=...`). The SPA MUST serve
 * that exact path, or every verification email 404s to the home redirect. This froze after a
 * route was briefly renamed to a localized path that broke the link.
 */
describe('routes — backend link contract', () => {
  it('serves the verification link at /verify-email', () => {
    const verify = routes.find((r) => r.path === 'verify-email');
    expect(verify).toBeDefined();
    expect(verify?.loadComponent).toBeTypeOf('function');
  });

  it('keeps a redirect for the legacy localized alias', () => {
    const alias = routes.find((r) => r.path === 'verificar-email');
    expect(alias?.redirectTo).toBe('verify-email');
  });
});
