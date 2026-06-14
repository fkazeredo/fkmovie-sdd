import { TranslateService } from '@ngx-translate/core';

import { ApiError } from './api-error.model';

/**
 * Resolves a user-facing message for an {@link ApiError}: a frontend i18n override under
 * `errors.<code>` when one exists (so the UI controls the wording, e.g. for rate limiting),
 * otherwise the backend's already-localized `message`. Returns '' for no error.
 */
export function resolveErrorText(
  translate: TranslateService,
  error: ApiError | null | undefined,
): string {
  if (!error) {
    return '';
  }
  const key = `errors.${error.code}`;
  const translated = translate.instant(key);
  return translated && translated !== key ? translated : error.message;
}
