import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';

import { ApiError } from '../../../../core/http/api-error.model';
import { resolveErrorText } from '../../../../core/http/error-text';
import { PageResponse } from '../../../screenings/screening.model';
import { MyReservation, ReservationStatus } from '../../reservation.model';
import { ReservationsApiService } from '../../reservations-api.service';

type Scope = 'upcoming' | 'all';
type TagSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';

const TWO_HOURS_MS = 2 * 60 * 60 * 1000;
const STATUS_SEVERITY: Record<ReservationStatus, TagSeverity> = {
  PENDING: 'warn',
  AWAITING_PAYMENT: 'info',
  CONFIRMED: 'success',
  CANCELLED: 'danger',
  EXPIRED: 'secondary',
};

/** The customer's reservation history with filters, pagination and cancellation (SPEC-0025). */
@Component({
  selector: 'app-my-reservations-page',
  imports: [
    RouterLink,
    TranslatePipe,
    ButtonModule,
    TagModule,
    ProgressSpinnerModule,
    ConfirmDialogModule,
  ],
  templateUrl: './my-reservations-page.html',
  providers: [ConfirmationService],
})
export class MyReservationsPage {
  private readonly api = inject(ReservationsApiService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly messages = inject(MessageService);
  private readonly translate = inject(TranslateService);

  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly page = signal<PageResponse<MyReservation> | null>(null);
  protected readonly scope = signal<Scope>('upcoming');
  protected readonly status = signal<ReservationStatus | null>(null);
  protected readonly pageIndex = signal(0);

  protected readonly statuses: (ReservationStatus | null)[] = [
    null,
    'PENDING',
    'AWAITING_PAYMENT',
    'CONFIRMED',
    'CANCELLED',
    'EXPIRED',
  ];

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api
      .list({
        upcoming: this.scope() === 'upcoming' ? true : undefined,
        status: this.status() ?? undefined,
        page: this.pageIndex(),
        size: 10,
      })
      .subscribe({
        next: (page) => {
          this.page.set(page);
          this.loading.set(false);
        },
        error: (err: ApiError) => {
          this.error.set(err);
          this.loading.set(false);
        },
      });
  }

  protected setScope(scope: Scope): void {
    this.scope.set(scope);
    this.pageIndex.set(0);
    this.load();
  }

  protected setStatus(status: ReservationStatus | null): void {
    this.status.set(status);
    this.pageIndex.set(0);
    this.load();
  }

  protected goToPage(index: number): void {
    this.pageIndex.set(index);
    this.load();
  }

  protected statusSeverity(status: ReservationStatus): TagSeverity {
    return STATUS_SEVERITY[status];
  }

  /** Mirrors the backend SPEC-0018 window (authority stays server-side; a 409 is handled on click). */
  protected cancelEligible(reservation: MyReservation): boolean {
    if (reservation.status === 'PENDING' || reservation.status === 'AWAITING_PAYMENT') {
      return true;
    }
    if (reservation.status === 'CONFIRMED') {
      return Date.parse(reservation.startsAt) - Date.now() >= TWO_HOURS_MS;
    }
    return false;
  }

  protected cancel(reservation: MyReservation): void {
    this.confirmation.confirm({
      header: this.translate.instant('minhasReservas.cancelTitle'),
      message: this.translate.instant('minhasReservas.cancelConfirm'),
      acceptLabel: this.translate.instant('minhasReservas.cancelYes'),
      rejectLabel: this.translate.instant('minhasReservas.cancelNo'),
      accept: () => this.doCancel(reservation),
    });
  }

  /** Performs the cancellation; surfaces the refund acknowledgment and refreshes the list. */
  protected doCancel(reservation: MyReservation): void {
    this.api.cancel(reservation.reservationId).subscribe({
      next: (result) => {
        const detail = result.refund.requested
          ? this.translate.instant('minhasReservas.refundAck', {
              amount: this.priceLabel(result.refund.amountCents),
            })
          : this.translate.instant('minhasReservas.cancelledDetail');
        this.messages.add({
          severity: 'success',
          summary: this.translate.instant('minhasReservas.cancelledTitle'),
          detail,
        });
        this.load();
      },
      error: (err: ApiError) => {
        this.messages.add({
          severity: 'error',
          summary: this.translate.instant('minhasReservas.cancelFailed'),
          detail: resolveErrorText(this.translate, err),
        });
        this.load();
      },
    });
  }

  protected priceLabel(cents: number): string {
    return (cents / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  protected dateLabel(iso: string): string {
    return new Date(iso).toLocaleString('pt-BR', {
      dateStyle: 'medium',
      timeStyle: 'short',
      timeZone: 'America/Sao_Paulo',
    });
  }

  protected errorText(): string {
    return resolveErrorText(this.translate, this.error());
  }
}
