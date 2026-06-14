import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';

import { AuthService } from '../../../../core/auth/auth.service';
import { ApiError } from '../../../../core/http/api-error.model';
import { resolveErrorText } from '../../../../core/http/error-text';

/** Password must be 8–100 chars with at least one letter and one digit (backend SPEC-0004). */
function passwordPolicy(control: AbstractControl): ValidationErrors | null {
  const value = String(control.value ?? '');
  return /^(?=.*[A-Za-z])(?=.*\d).{8,100}$/.test(value) ? null : { passwordPolicy: true };
}

/** Cross-field: password and confirmation must match. */
function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  return group.get('password')?.value === group.get('confirm')?.value
    ? null
    : { passwordMismatch: true };
}

/** Customer self-registration (SPEC-0021): on success auto-logs in and lands on the sessions list. */
@Component({
  selector: 'app-register-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    TranslatePipe,
    ButtonModule,
    InputTextModule,
    MessageModule,
  ],
  templateUrl: './register-page.html',
})
export class RegisterPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  protected readonly loading = signal(false);
  protected readonly error = signal<ApiError | null>(null);

  protected readonly form = this.fb.nonNullable.group(
    {
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, passwordPolicy]],
      confirm: ['', [Validators.required]],
    },
    { validators: passwordsMatch },
  );

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const { name, email, password } = this.form.getRawValue();
    this.auth.register(name, email, password).subscribe({
      next: () => {
        this.loading.set(false);
        void this.router.navigateByUrl('/sessoes');
      },
      error: (err: ApiError) => {
        this.loading.set(false);
        this.error.set(err);
      },
    });
  }

  protected errorText(): string {
    return resolveErrorText(this.translate, this.error());
  }
}
