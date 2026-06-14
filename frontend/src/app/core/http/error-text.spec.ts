import { TranslateService } from '@ngx-translate/core';

import { ApiError } from './api-error.model';
import { resolveErrorText } from './error-text';

function fakeTranslate(map: Record<string, string>): TranslateService {
  return { instant: (key: string) => map[key] ?? key } as unknown as TranslateService;
}

function err(code: string, message: string): ApiError {
  return { code, message, fields: [], status: 400 };
}

describe('resolveErrorText', () => {
  it('prefers a frontend i18n override keyed by the backend code', () => {
    const translate = fakeTranslate({ 'errors.auth.invalid-credentials': 'E-mail ou senha inválidos.' });
    expect(resolveErrorText(translate, err('auth.invalid-credentials', 'server'))).toBe(
      'E-mail ou senha inválidos.',
    );
  });

  it('falls back to the backend message when there is no override', () => {
    expect(resolveErrorText(fakeTranslate({}), err('some.unmapped', 'server message'))).toBe('server message');
  });

  it('returns an empty string when there is no error', () => {
    expect(resolveErrorText(fakeTranslate({}), null)).toBe('');
  });
});
