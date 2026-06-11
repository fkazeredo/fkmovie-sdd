import { TestBed } from '@angular/core/testing';
import { TranslateService, provideTranslateService } from '@ngx-translate/core';

import { LanguageService } from './language.service';

describe('LanguageService', () => {
  let service: LanguageService;
  let translate: TranslateService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideTranslateService()],
    });
    service = TestBed.inject(LanguageService);
    translate = TestBed.inject(TranslateService);
  });

  it('defaults to pt-BR on init when nothing is persisted', () => {
    service.init();
    expect(service.current()).toBe('pt-BR');
    expect(translate.getCurrentLang()).toBe('pt-BR');
  });

  it('switches the active language at runtime and persists the choice', () => {
    service.init();
    service.use('en');

    expect(service.current()).toBe('en');
    expect(translate.getCurrentLang()).toBe('en');
    expect(localStorage.getItem('fkmovies.language')).toBe('en');
  });

  it('restores the persisted language on init', () => {
    localStorage.setItem('fkmovies.language', 'en');
    service.init();
    expect(service.current()).toBe('en');
  });
});
