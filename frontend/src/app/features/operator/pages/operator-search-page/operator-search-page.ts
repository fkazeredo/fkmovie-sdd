import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';

import { ApiError } from '../../../../core/http/api-error.model';
import { resolveErrorText } from '../../../../core/http/error-text';
import {
  OperatorReservation,
  OperatorReservationDetail,
  SearchCriterion,
} from '../../operator.model';
import { OperatorApiService } from '../../operator-api.service';

/** Operator console: find a reservation by one criterion and open a ticket to reprint (SPEC-0026/0020). */
@Component({
  selector: 'app-operator-search-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    TranslatePipe,
    ButtonModule,
    InputTextModule,
    ProgressSpinnerModule,
    TagModule,
  ],
  templateUrl: './operator-search-page.html',
})
export class OperatorSearchPage {
  private readonly api = inject(OperatorApiService);
  private readonly translate = inject(TranslateService);
  private readonly fb = inject(FormBuilder);

  protected readonly criteria: SearchCriterion[] = ['ticketCode', 'reservationId', 'email'];
  protected readonly criterion = signal<SearchCriterion>('ticketCode');
  // A FormGroup (not a bare FormControl) so the <form> carries FormGroupDirective and (ngSubmit)
  // fires with preventDefault — otherwise the submit button does a native page reload.
  protected readonly form = this.fb.nonNullable.group({ query: [''] });

  protected readonly loading = signal(false);
  protected readonly searched = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly results = signal<OperatorReservation[]>([]);
  protected readonly detail = signal<OperatorReservationDetail | null>(null);
  protected readonly detailLoading = signal(false);

  protected setCriterion(criterion: SearchCriterion): void {
    this.criterion.set(criterion);
  }

  protected search(): void {
    const value = this.form.getRawValue().query.trim();
    if (!value) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.detail.set(null);
    this.searched.set(true);
    this.api.search(this.criterion(), value).subscribe({
      next: (results) => {
        this.results.set(results);
        this.loading.set(false);
      },
      error: (err: ApiError) => {
        this.error.set(err);
        this.results.set([]);
        this.loading.set(false);
      },
    });
  }

  protected openDetail(reservation: OperatorReservation): void {
    this.detailLoading.set(true);
    this.detail.set(null);
    this.api.detail(reservation.reservationId).subscribe({
      next: (detail) => {
        this.detail.set(detail);
        this.detailLoading.set(false);
      },
      error: () => this.detailLoading.set(false),
    });
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
