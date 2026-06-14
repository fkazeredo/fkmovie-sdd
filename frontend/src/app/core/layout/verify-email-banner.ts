import { Component, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';

import { AuthService } from '../auth/auth.service';

/**
 * Persistent banner shown to a logged-in customer whose email is not yet verified (SPEC-0021).
 * Offers a one-click resend; the backend is deliberately silent, so success is assumed.
 */
@Component({
  selector: 'app-verify-email-banner',
  imports: [TranslatePipe, ButtonModule],
  template: `
    @if (auth.isAuthenticated() && !auth.emailVerified()) {
      <div
        class="flex flex-wrap items-center justify-center gap-x-3 gap-y-1 border-b border-amber-400/20 bg-amber-400/10 px-4 py-2 text-center text-sm text-amber-200"
      >
        <i class="pi pi-envelope"></i>
        <span>{{ 'auth.banner.unverified' | translate }}</span>
        @if (sent()) {
          <span class="font-medium text-amber-100">{{ 'auth.banner.sent' | translate }}</span>
        } @else {
          <p-button
            [label]="'auth.banner.resend' | translate"
            size="small"
            [text]="true"
            severity="warn"
            (onClick)="resend()"
          />
        }
      </div>
    }
  `,
})
export class VerifyEmailBanner {
  protected readonly auth = inject(AuthService);
  protected readonly sent = signal(false);

  protected resend(): void {
    const email = this.auth.user()?.email;
    if (!email) {
      return;
    }
    this.auth.resendVerification(email).subscribe({
      next: () => this.sent.set(true),
      error: () => this.sent.set(true),
    });
  }
}
