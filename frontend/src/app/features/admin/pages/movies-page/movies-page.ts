import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';

import { ApiError } from '../../../../core/http/api-error.model';
import { resolveErrorText } from '../../../../core/http/error-text';
import { PageResponse } from '../../../screenings/screening.model';
import { AGE_RATINGS, Movie } from '../../admin.model';
import { AdminApiService } from '../../admin-api.service';

/** Admin movie catalog: list, create/edit dialog and archive toggle (SPEC-0026 / backend 0008). */
@Component({
  selector: 'app-movies-page',
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
  templateUrl: './movies-page.html',
  providers: [ConfirmationService],
})
export class MoviesPage {
  private readonly api = inject(AdminApiService);
  private readonly fb = inject(FormBuilder);
  private readonly messages = inject(MessageService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);

  protected readonly ageRatings = AGE_RATINGS;
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly page = signal<PageResponse<Movie> | null>(null);
  protected readonly dialogOpen = signal(false);
  protected readonly editing = signal<Movie | null>(null);
  protected readonly saving = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    durationMinutes: [120, [Validators.required, Validators.min(1), Validators.max(600)]],
    ageRating: ['A14' as Movie['ageRating'], [Validators.required]],
    synopsis: [''],
    posterUrl: [''],
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listMovies({ size: 50 }).subscribe({
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

  protected openCreate(): void {
    this.editing.set(null);
    this.form.reset({
      title: '',
      durationMinutes: 120,
      ageRating: 'A14',
      synopsis: '',
      posterUrl: '',
    });
    this.dialogOpen.set(true);
  }

  protected openEdit(movie: Movie): void {
    this.editing.set(movie);
    this.form.reset({
      title: movie.title,
      durationMinutes: movie.durationMinutes,
      ageRating: movie.ageRating,
      synopsis: movie.synopsis ?? '',
      posterUrl: movie.posterUrl ?? '',
    });
    this.dialogOpen.set(true);
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const raw = this.form.getRawValue();
    const request = {
      title: raw.title,
      durationMinutes: raw.durationMinutes,
      ageRating: raw.ageRating,
      synopsis: raw.synopsis || null,
      posterUrl: raw.posterUrl || null,
    };
    const editing = this.editing();
    const call = editing
      ? this.api.updateMovie(editing.id, request)
      : this.api.createMovie(request);
    call.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogOpen.set(false);
        this.toast('success', 'admin.movies.saved');
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

  protected toggleArchive(movie: Movie): void {
    const archiving = movie.status === 'ACTIVE';
    this.confirmation.confirm({
      header: this.translate.instant(archiving ? 'admin.movies.archive' : 'admin.movies.unarchive'),
      message: this.translate.instant(
        archiving ? 'admin.movies.archiveConfirm' : 'admin.movies.unarchiveConfirm',
      ),
      accept: () => {
        const call = archiving
          ? this.api.archiveMovie(movie.id)
          : this.api.unarchiveMovie(movie.id);
        call.subscribe({
          next: () => {
            this.toast('success', 'admin.movies.saved');
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

  protected errorText(): string {
    return resolveErrorText(this.translate, this.error());
  }

  private toast(severity: string, key: string): void {
    this.messages.add({ severity, summary: this.translate.instant(key) });
  }
}
