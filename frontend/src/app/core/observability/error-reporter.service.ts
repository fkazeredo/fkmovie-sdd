import { Injectable } from '@angular/core';

/**
 * Central error reporting (spec 0002): console-only in the foundation. Production
 * integration (Sentry or equivalent) plugs in here without touching call sites.
 */
@Injectable({ providedIn: 'root' })
export class ErrorReporterService {
  report(error: unknown): void {
    console.error('[fkmovies]', error);
  }
}
