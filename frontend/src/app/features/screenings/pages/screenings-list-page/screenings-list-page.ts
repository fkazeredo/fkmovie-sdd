import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';

import { ApiError } from '../../../../core/http/api-error.model';
import { PublicScreening } from '../../screening.model';
import { ScreeningsService } from '../../screenings.service';

/** Public, anonymous listing of upcoming sessions (frontend SPEC-0022). */
@Component({
  selector: 'app-screenings-list-page',
  imports: [RouterLink, TranslatePipe, ButtonModule, ProgressSpinnerModule, TagModule],
  templateUrl: './screenings-list-page.html',
})
export class ScreeningsListPage {
  private readonly service = inject(ScreeningsService);

  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly screenings = signal<PublicScreening[]>([]);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.list({ size: 50 }).subscribe({
      next: (page) => {
        this.screenings.set(page.content);
        this.loading.set(false);
      },
      error: (err: ApiError) => {
        this.error.set(err?.message ?? 'error');
        this.loading.set(false);
      },
    });
  }

  protected priceLabel(cents: number): string {
    return (cents / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  protected dateLabel(iso: string): string {
    return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'medium', timeStyle: 'short' });
  }

  /** Hides a broken poster image so the gradient placeholder behind it shows instead. */
  protected hideImage(event: Event): void {
    (event.target as HTMLImageElement).style.visibility = 'hidden';
  }
}
