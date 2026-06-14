import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';

import { ApiError } from '../../../../core/http/api-error.model';
import { resolveErrorText } from '../../../../core/http/error-text';
import { AdminScreening, Movie, Room } from '../../admin.model';
import { AdminApiService } from '../../admin-api.service';
import { spLocalToUtcIso } from '../../sp-time';

/** Admin screening scheduler: list and create sessions (SPEC-0026 / backend 0009). */
@Component({
  selector: 'app-screenings-admin-page',
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    ButtonModule,
    DialogModule,
    TagModule,
    ProgressSpinnerModule,
    InputTextModule,
    ConfirmDialogModule,
  ],
  templateUrl: './screenings-admin-page.html',
  providers: [ConfirmationService],
})
export class ScreeningsAdminPage {
  private readonly api = inject(AdminApiService);
  private readonly fb = inject(FormBuilder);
  private readonly messages = inject(MessageService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);

  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly screenings = signal<AdminScreening[]>([]);
  protected readonly movies = signal<Movie[]>([]);
  protected readonly rooms = signal<Room[]>([]);
  protected readonly dialogOpen = signal(false);
  protected readonly saving = signal(false);

  protected readonly activeMovies = computed(() =>
    this.movies().filter((m) => m.status === 'ACTIVE'),
  );
  private readonly movieTitles = computed(() => new Map(this.movies().map((m) => [m.id, m.title])));
  private readonly roomNames = computed(() => new Map(this.rooms().map((r) => [r.id, r.name])));

  protected readonly form = this.fb.nonNullable.group({
    movieId: ['', [Validators.required]],
    roomId: ['', [Validators.required]],
    startsAtLocal: ['', [Validators.required]],
    basePriceReais: [30, [Validators.required, Validators.min(0.01)]],
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      screenings: this.api.listScreenings({ size: 100 }),
      movies: this.api.listMovies({ size: 100 }),
      rooms: this.api.listRooms(),
    }).subscribe({
      next: ({ screenings, movies, rooms }) => {
        this.screenings.set(screenings.content);
        this.movies.set(movies.content);
        this.rooms.set(rooms);
        this.loading.set(false);
      },
      error: (err: ApiError) => {
        this.error.set(err);
        this.loading.set(false);
      },
    });
  }

  protected movieTitle(id: string): string {
    return this.movieTitles().get(id) ?? id;
  }

  protected roomName(id: string): string {
    return this.roomNames().get(id) ?? id;
  }

  protected openCreate(): void {
    this.form.reset({ movieId: '', roomId: '', startsAtLocal: '', basePriceReais: 30 });
    this.dialogOpen.set(true);
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const raw = this.form.getRawValue();
    this.api
      .createScreening({
        movieId: raw.movieId,
        roomId: raw.roomId,
        startsAt: spLocalToUtcIso(raw.startsAtLocal),
        basePriceCents: Math.round(raw.basePriceReais * 100),
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.dialogOpen.set(false);
          this.toast('success', 'admin.screenings.saved');
          this.load();
        },
        error: (err: ApiError) => {
          this.saving.set(false);
          this.messages.add({
            severity: 'error',
            summary: this.translate.instant('admin.saveFailed'),
            detail: resolveErrorText(this.translate, err),
          });
        },
      });
  }

  protected cancel(screening: AdminScreening): void {
    this.confirmation.confirm({
      header: this.translate.instant('admin.screenings.cancel'),
      message: this.translate.instant('admin.screenings.cancelConfirm'),
      accept: () => {
        this.api.cancelScreening(screening.id).subscribe({
          next: () => {
            this.toast('success', 'admin.screenings.cancelled');
            this.load();
          },
          error: (err: ApiError) =>
            this.messages.add({
              severity: 'error',
              summary: this.translate.instant('admin.saveFailed'),
              detail: resolveErrorText(this.translate, err),
            }),
        });
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

  private toast(severity: string, key: string): void {
    this.messages.add({ severity, summary: this.translate.instant(key) });
  }
}
