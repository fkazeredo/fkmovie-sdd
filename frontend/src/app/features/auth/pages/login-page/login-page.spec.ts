import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AuthUser } from '../../../../core/auth/auth.model';
import { AuthService } from '../../../../core/auth/auth.service';
import { ApiError } from '../../../../core/http/api-error.model';
import { LoginPage } from './login-page';

const customer: AuthUser = {
  id: 'u1',
  email: 'a@b.com',
  name: 'Ana',
  role: 'CUSTOMER',
  emailVerified: true,
};
const admin: AuthUser = { ...customer, role: 'ADMIN' };

async function render(
  authStub: Partial<AuthService>,
  queryParams: Record<string, string> = {},
): Promise<ComponentFixture<LoginPage>> {
  await TestBed.configureTestingModule({
    imports: [LoginPage],
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
  const fixture = TestBed.createComponent(LoginPage);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function instance(fixture: ComponentFixture<LoginPage>): any {
  return fixture.componentInstance;
}

describe('LoginPage', () => {
  it('does not call login while the form is invalid', async () => {
    const login = vi.fn();
    const fixture = await render({ login });
    instance(fixture).submit();
    expect(login).not.toHaveBeenCalled();
  });

  it('logs in and redirects a customer to /sessoes', async () => {
    const fixture = await render({ login: () => of(customer) });
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    instance(fixture).form.setValue({ email: 'a@b.com', password: 'secret1' });
    instance(fixture).submit();

    expect(navigate).toHaveBeenCalledWith('/sessoes');
  });

  it('honors the returnUrl query parameter', async () => {
    const fixture = await render(
      { login: () => of(customer) },
      { returnUrl: '/sessoes/1/assentos' },
    );
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    instance(fixture).form.setValue({ email: 'a@b.com', password: 'secret1' });
    instance(fixture).submit();

    expect(navigate).toHaveBeenCalledWith('/sessoes/1/assentos');
  });

  it('routes an admin to /admin', async () => {
    const fixture = await render({ login: () => of(admin) });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    instance(fixture).form.setValue({ email: 'a@b.com', password: 'secret1' });
    instance(fixture).submit();

    expect(navigate).toHaveBeenCalledWith('/admin');
  });

  it('shows the backend error message on failure', async () => {
    const error: ApiError = {
      code: 'auth.invalid-credentials',
      message: 'Credenciais inválidas',
      fields: [],
      status: 401,
    };
    const fixture = await render({ login: () => throwError(() => error) });

    instance(fixture).form.setValue({ email: 'a@b.com', password: 'wrong1' });
    instance(fixture).submit();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Credenciais inválidas');
  });
});
