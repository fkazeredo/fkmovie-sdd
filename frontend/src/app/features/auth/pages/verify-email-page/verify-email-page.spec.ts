import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AuthService } from '../../../../core/auth/auth.service';
import { ApiError } from '../../../../core/http/api-error.model';
import { VerifyEmailPage } from './verify-email-page';

async function render(
  authStub: Partial<AuthService>,
  queryParams: Record<string, string> = {},
): Promise<ComponentFixture<VerifyEmailPage>> {
  await TestBed.configureTestingModule({
    imports: [VerifyEmailPage],
    providers: [
      provideRouter([]),
      provideTranslateService(),
      { provide: AuthService, useValue: authStub },
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { queryParamMap: convertToParamMap(queryParams) } },
      },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(VerifyEmailPage);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function instance(fixture: ComponentFixture<VerifyEmailPage>): any {
  return fixture.componentInstance;
}

describe('VerifyEmailPage', () => {
  it('shows the no-token state when the link has no token', async () => {
    const fixture = await render({});
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'auth.verify.noTokenTitle',
    );
  });

  it('verifies the token on load and shows success', async () => {
    const verifyEmail = vi.fn().mockReturnValue(of({ email: 'a@b.com', emailVerified: true }));
    const fixture = await render({ verifyEmail }, { token: 'abc' });
    expect(verifyEmail).toHaveBeenCalledWith('abc');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'auth.verify.successTitle',
    );
  });

  it('shows the error state and offers a resend on an expired token', async () => {
    const error: ApiError = {
      code: 'user.token-expired',
      message: 'Link expirou',
      fields: [],
      status: 410,
    };
    const resendVerification = vi.fn().mockReturnValue(of(void 0));
    const fixture = await render(
      { verifyEmail: () => throwError(() => error), resendVerification },
      { token: 'old' },
    );

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Link expirou');

    instance(fixture).resendForm.setValue({ email: 'a@b.com' });
    instance(fixture).resend();

    expect(resendVerification).toHaveBeenCalledWith('a@b.com');
  });
});
