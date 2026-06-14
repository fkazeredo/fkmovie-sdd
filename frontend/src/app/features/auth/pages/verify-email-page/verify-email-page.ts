import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { AuthService } from '../../../../core/auth/auth.service';
import { ApiError } from '../../../../core/http/api-error.model';
import { resolveErrorText } from '../../../../core/http/error-text';

type State = 'loading' | 'success' | 'error' | 'no-token';

/** Email verification (SPEC-0021): consumes the token from the query string on load. */
@Component({
  selector: 'app-verify-email-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    TranslatePipe,
    ButtonModule,
    InputTextModule,
    MessageModule,
    ProgressSpinnerModule,
  ],
  templateUrl: './verify-email-page.html',
})
export class VerifyEmailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  protected readonly state = signal<State>('loading');
  protected readonly error = signal<ApiError | null>(null);
  protected readonly resent = signal(false);

  protected readonly resendForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  constructor() {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.state.set('no-token');
      return;
    }
    this.auth.verifyEmail(token).subscribe({
      next: () => this.state.set('success'),
      error: (err: ApiError) => {
        this.error.set(err);
        this.state.set('error');
      },
    });
  }

  protected resend(): void {
    if (this.resendForm.invalid) {
      this.resendForm.markAllAsTouched();
      return;
    }
    this.auth.resendVerification(this.resendForm.getRawValue().email).subscribe({
      // Anti-enumeration: the backend is intentionally silent, so always confirm.
      next: () => this.resent.set(true),
      error: () => this.resent.set(true),
    });
  }

  protected errorText(): string {
    return resolveErrorText(this.translate, this.error());
  }
}
