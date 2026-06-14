import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AuthUser } from '../../../../core/auth/auth.model';
import { AuthService } from '../../../../core/auth/auth.service';
import { ApiError } from '../../../../core/http/api-error.model';
import { RegisterPage } from './register-page';

const customer: AuthUser = {
  id: 'u1',
  email: 'a@b.com',
  name: 'Ana',
  role: 'CUSTOMER',
  emailVerified: false,
};

async function render(authStub: Partial<AuthService>): Promise<ComponentFixture<RegisterPage>> {
  await TestBed.configureTestingModule({
    imports: [RegisterPage],
    providers: [
      provideRouter([]),
      provideTranslateService(),
      { provide: AuthService, useValue: authStub },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(RegisterPage);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function instance(fixture: ComponentFixture<RegisterPage>): any {
  return fixture.componentInstance;
}

const valid = { name: 'Ana', email: 'ana@example.com', password: 'secret12', confirm: 'secret12' };

describe('RegisterPage', () => {
  it('does not register when the passwords do not match', async () => {
    const registerFn = vi.fn();
    const fixture = await render({ register: registerFn });
    instance(fixture).form.setValue({ ...valid, confirm: 'different1' });
    instance(fixture).submit();
    expect(registerFn).not.toHaveBeenCalled();
  });

  it('does not register with a weak password', async () => {
    const registerFn = vi.fn();
    const fixture = await render({ register: registerFn });
    instance(fixture).form.setValue({
      name: 'Ana',
      email: 'ana@example.com',
      password: 'short',
      confirm: 'short',
    });
    instance(fixture).submit();
    expect(registerFn).not.toHaveBeenCalled();
  });

  it('registers a valid form and lands on /sessoes', async () => {
    const registerFn = vi.fn().mockReturnValue(of(customer));
    const fixture = await render({ register: registerFn });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    instance(fixture).form.setValue(valid);
    instance(fixture).submit();

    expect(registerFn).toHaveBeenCalledWith('Ana', 'ana@example.com', 'secret12');
    expect(navigate).toHaveBeenCalledWith('/sessoes');
  });

  it('shows the email-taken error from the backend', async () => {
    const error: ApiError = {
      code: 'user.email-taken',
      message: 'E-mail já cadastrado',
      fields: [],
      status: 409,
    };
    const fixture = await render({ register: () => throwError(() => error) });

    instance(fixture).form.setValue(valid);
    instance(fixture).submit();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('E-mail já cadastrado');
  });
});
