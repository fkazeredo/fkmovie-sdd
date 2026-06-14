import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { ApiError } from '../../../../core/http/api-error.model';
import { SeatMap, SeatMapSeat } from '../../screening.model';
import { ScreeningsService } from '../../screenings.service';

interface SeatRow {
  row: string;
  seats: SeatMapSeat[];
}

/** Read-only seat map of a screening with client-side seat selection (frontend SPEC-0023). */
@Component({
  selector: 'app-seat-map-page',
  imports: [RouterLink, TranslatePipe, ButtonModule, ProgressSpinnerModule],
  templateUrl: './seat-map-page.html',
})
export class SeatMapPage {
  private readonly service = inject(ScreeningsService);
  private readonly route = inject(ActivatedRoute);

  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly map = signal<SeatMap | null>(null);
  protected readonly selected = signal<string[]>([]);

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
    if (seat.status === 'SOLD') {
      return 'cursor-not-allowed bg-surface-300 text-surface-500';
    }
    if (seat.status === 'HELD') {
      return 'cursor-not-allowed bg-amber-300 text-amber-900';
    }
    return this.isSelected(seat)
      ? 'bg-primary text-primary-contrast ring-2 ring-primary'
      : 'bg-surface-0 text-surface-700 ring-1 ring-surface-300 hover:ring-primary';
  }

  protected priceLabel(cents: number): string {
    return (cents / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  protected dateLabel(iso: string): string {
    return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'medium', timeStyle: 'short' });
  }
}
