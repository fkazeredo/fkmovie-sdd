import { Injectable, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export const SUPPORTED_LANGUAGES = ['pt-BR', 'en'] as const;
export type SupportedLanguage = (typeof SUPPORTED_LANGUAGES)[number];

const STORAGE_KEY = 'fkmovies.language';

/**
 * Runtime language switching (spec 0002, ADR 0008): default `pt-BR`, fallback `en`.
 * The choice persists across sessions in localStorage.
 */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translate = inject(TranslateService);
  private readonly currentLanguage = signal<SupportedLanguage>('pt-BR');

  readonly current = this.currentLanguage.asReadonly();

  /** Applies the persisted language (or the pt-BR default) at application startup. */
  init(): void {
    const stored = localStorage.getItem(STORAGE_KEY);
    this.use(isSupported(stored) ? stored : 'pt-BR');
  }

  use(language: SupportedLanguage): void {
    this.translate.use(language);
    this.currentLanguage.set(language);
    localStorage.setItem(STORAGE_KEY, language);
  }
}

function isSupported(value: string | null): value is SupportedLanguage {
  return value !== null && (SUPPORTED_LANGUAGES as readonly string[]).includes(value);
}
