import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { interval, timer } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';

import { AuthService } from '../../../../core/auth/auth.service';
import { ApiError } from '../../../../core/http/api-error.model';
import { resolveErrorText } from '../../../../core/http/error-text';
import { Reservation, ReservationSnapshot } from '../../reservation.model';
import { ReservationsApiService } from '../../reservations-api.service';

const TERMINAL: ReadonlySet<string> = new Set(['CONFIRMED', 'CANCELLED', 'EXPIRED']);

/** Status-driven reservation flow: hold → confirm → async payment → tickets (frontend SPEC-0024). */
@Component({
  selector: 'app-reservation-page',
  imports: [RouterLink, TranslatePipe, ButtonModule, ProgressSpinnerModule, TagModule],
  templateUrl: './reservation-page.html',
  host: { '(window:focus)': 'onFocus()' },
})
export class ReservationPage {
  private readonly api = inject(ReservationsApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly auth = inject(AuthService);

  private readonly id = this.route.snapshot.paramMap.get('id') ?? '';
  private polling = false;
  private reloading = false;

  protected readonly reservation = signal<Reservation | null>(null);
  protected readonly serverOffsetMs = signal(0);
  protected readonly loading = signal(true);
  protected readonly acting = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly nowMs = signal(Date.now());

  /** The active deadline for the current status (hold expiry while PENDING, payment otherwise). */
  protected readonly deadlineIso = computed<string | null>(() => {
    const r = this.reservation();
    if (!r) {
      return null;
    }
    if (r.status === 'PENDING') {
      return r.expiresAt;
    }
    if (r.status === 'AWAITING_PAYMENT') {
      return r.paymentDeadlineAt;
    }
    return null;
  });

  /** Remaining ms to the deadline, anchored to server time (skew-resistant); null when none. */
  protected readonly remainingMs = computed<number | null>(() => {
    const iso = this.deadlineIso();
    if (!iso) {
      return null;
    }
    const remaining = Date.parse(iso) - (this.nowMs() + this.serverOffsetMs());
    return remaining > 0 ? remaining : 0;
  });

  protected readonly countdownLabel = computed<string>(() => {
    const ms = this.remainingMs();
    if (ms === null) {
      return '';
    }
    const totalSeconds = Math.floor(ms / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
  });

  constructor() {
    interval(1000)
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        this.nowMs.set(Date.now());
        const r = this.reservation();
        if (r && r.status === 'PENDING' && this.remainingMs() === 0) {
          this.reload();
        }
      });
    this.reload();
  }

  /** Refetches the reservation; re-anchors the clock and resumes payment polling if needed. */
  protected reload(): void {
    if (this.reloading) {
      return;
    }
    this.reloading = true;
    this.api.get(this.id).subscribe({
      next: (snapshot) => {
        this.reloading = false;
        this.applySnapshot(snapshot);
      },
      error: (err: ApiError) => {
        this.reloading = false;
        this.error.set(err);
        this.loading.set(false);
      },
    });
  }

  protected confirm(): void {
    if (this.acting()) {
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    this.api.confirm(this.id).subscribe({
      next: () => {
        this.acting.set(false);
        this.reload();
      },
      error: (err: ApiError) => {
        this.acting.set(false);
        this.error.set(err);
        this.reload();
      },
    });
  }

  protected cancel(): void {
    if (this.acting()) {
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    this.api.cancel(this.id).subscribe({
      next: () => {
        this.acting.set(false);
        this.reload();
      },
      error: (err: ApiError) => {
        this.acting.set(false);
        this.error.set(err);
        this.reload();
      },
    });
  }

  /** Re-syncs from the server when the tab regains focus (resists a stale countdown). */
  protected onFocus(): void {
    const r = this.reservation();
    if (r && !TERMINAL.has(r.status)) {
      this.reload();
    }
  }

  protected priceLabel(cents: number): string {
    return (cents / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  protected dateLabel(iso: string): string {
    return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'medium', timeStyle: 'short' });
  }

  protected errorText(): string {
    return resolveErrorText(this.translate, this.error());
  }

  private applySnapshot(snapshot: ReservationSnapshot): void {
    this.reservation.set(snapshot.reservation);
    this.serverOffsetMs.set(snapshot.serverOffsetMs);
    this.loading.set(false);
    if (snapshot.reservation.status === 'AWAITING_PAYMENT') {
      this.startPaymentPolling();
    }
  }

  /**
   * Polls the reservation every 2 s while the async payment settles (SPEC-0024 fallback for the
   * realtime queue). Completes as soon as the status leaves AWAITING_PAYMENT.
   */
  private startPaymentPolling(): void {
    if (this.polling) {
      return;
    }
    this.polling = true;
    timer(2000, 2000)
      .pipe(
        switchMap(() => this.api.get(this.id)),
        takeWhile((snapshot) => snapshot.reservation.status === 'AWAITING_PAYMENT', true),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (snapshot) => {
          this.reservation.set(snapshot.reservation);
          this.serverOffsetMs.set(snapshot.serverOffsetMs);
        },
        complete: () => (this.polling = false),
        error: () => (this.polling = false),
      });
  }
}
