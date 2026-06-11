import { ErrorHandler, Injectable, inject } from '@angular/core';

import { ErrorReporterService } from './error-reporter.service';

/** Routes uncaught errors through the central reporter instead of the default console dump. */
@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private readonly reporter = inject(ErrorReporterService);

  handleError(error: unknown): void {
    this.reporter.report(error);
  }
}
