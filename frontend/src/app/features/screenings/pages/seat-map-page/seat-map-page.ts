import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { AuthService } from '../../../../core/auth/auth.service';
import { ApiError } from '../../../../core/http/api-error.model';
import { resolveErrorText } from '../../../../core/http/error-text';
import { CreateReservationSeat } from '../../../reservation/reservation.model';
import { ReservationsApiService } from '../../../reservation/reservations-api.service';
import { SeatMap, SeatMapSeat } from '../../screening.model';
import { ScreeningsService } from '../../screenings.service';

interface SeatRow {
  row: string;
  seats: SeatMapSeat[];
}

/** Seat map with selection that hands a hold off to the reservation flow (frontend SPEC-0023/0024). */
@Component({
  selector: 'app-seat-map-page',
  imports: [RouterLink, TranslatePipe, ButtonModule, ProgressSpinnerModule],
  templateUrl: './seat-map-page.html',
})
export class SeatMapPage {
  private readonly service = inject(ScreeningsService);
  private readonly reservations = inject(ReservationsApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  protected readonly auth = inject(AuthService);

  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly map = signal<SeatMap | null>(null);
  protected readonly selected = signal<string[]>([]);
  protected readonly reserving = signal(false);
  protected readonly reserveError = signal<ApiError | null>(null);
  /** Seats reported stolen by a seats-unavailable rejection, highlighted on the map. */
  protected readonly unavailable = signal<string[]>([]);

  protected readonly rows = computed<SeatRow[]>(() => {
    const seats = this.map()?.seats ?? [];
    const byRow = new Map<string, SeatMapSeat[]>();
    for (const seat of seats) {
      (byRow.get(seat.row) ?? byRow.set(seat.row, []).get(seat.row)!).push(seat);
    }
    return [...byRow.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([row, rowSeats]) => ({
        row,
        seats: [...rowSeats].sort((a, b) => a.number - b.number),
      }));
  });

  protected readonly totalCents = computed(() => {
    const chosen = new Set(this.selected());
    return (this.map()?.seats ?? [])
      .filter((s) => chosen.has(s.seatId))
      .reduce((sum, s) => sum + s.fullPriceCents, 0);
  });

  protected readonly canReserve = computed(
    () => this.auth.isAuthenticated() && this.auth.emailVerified() && this.selected().length > 0,
  );

  constructor() {
    this.load();
  }

  protected load(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('missing id');
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.service.seatMap(id).subscribe({
      next: (map) => {
        this.map.set(map);
        this.loading.set(false);
      },
      error: (err: ApiError) => {
        this.error.set(err?.message ?? 'error');
        this.loading.set(false);
      },
    });
  }

  protected toggle(seat: SeatMapSeat): void {
    if (seat.status !== 'FREE') {
      return;
    }
    const chosen = this.selected();
    this.selected.set(
      chosen.includes(seat.seatId)
        ? chosen.filter((id) => id !== seat.seatId)
        : [...chosen, seat.seatId],
    );
  }

  protected isSelected(seat: SeatMapSeat): boolean {
    return this.selected().includes(seat.seatId);
  }

  protected seatClass(seat: SeatMapSeat): string {
    if (this.unavailable().includes(seat.seatId)) {
      return 'cursor-not-allowed bg-red-500/30 text-red-100 ring-2 ring-red-400';
    }
    if (seat.status === 'SOLD') {
      return 'cursor-not-allowed bg-surface-700 text-surface-500';
    }
    if (seat.status === 'HELD') {
      return 'cursor-not-allowed bg-amber-400/70 text-amber-950';
    }
    return this.isSelected(seat)
      ? 'bg-primary text-primary-contrast ring-2 ring-primary'
      : 'bg-surface-800 text-surface-200 ring-1 ring-surface-600 hover:ring-primary';
  }

  /** Reserves the selected seats and hands off to the reservation page (FULL tickets in v1). */
  protected reserve(): void {
    const map = this.map();
    if (!map || this.selected().length === 0 || this.reserving()) {
      return;
    }
    if (!this.auth.isAuthenticated()) {
      void this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    this.reserving.set(true);
    this.reserveError.set(null);
    this.unavailable.set([]);
    const seats: CreateReservationSeat[] = this.selected().map((seatId) => ({
      seatId,
      ticketType: 'FULL',
    }));
    this.reservations.create(map.screeningId, seats).subscribe({
      next: (reservation) => {
        this.reserving.set(false);
        void this.router.navigate(['/reservas', reservation.reservationId]);
      },
      error: (err: ApiError) => {
        this.reserving.set(false);
        this.reserveError.set(err);
        if (err.code === 'booking.seats-unavailable') {
          this.unavailable.set(
            err.fields.filter((f) => f.field === 'seatId').map((f) => f.message),
          );
          this.selected.set([]);
          this.load();
        }
      },
    });
  }

  protected reserveErrorText(): string {
    return resolveErrorText(this.translate, this.reserveError());
  }

  protected priceLabel(cents: number): string {
    return (cents / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  protected dateLabel(iso: string): string {
    return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'medium', timeStyle: 'short' });
  }
}
