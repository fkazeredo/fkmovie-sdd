import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { ApiError } from '../../../../core/http/api-error.model';
import { resolveErrorText } from '../../../../core/http/error-text';
import { Reprint } from '../../operator.model';
import { OperatorApiService } from '../../operator-api.service';

/** Print-friendly ticket reprint (SPEC-0026/0020): reprints on load and triggers window.print(). */
@Component({
  selector: 'app-ticket-print-page',
  imports: [RouterLink, TranslatePipe, ButtonModule, ProgressSpinnerModule],
  templateUrl: './ticket-print-page.html',
  host: { class: 'print-page' },
})
export class TicketPrintPage {
  private readonly api = inject(OperatorApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly translate = inject(TranslateService);

  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly ticket = signal<Reprint | null>(null);

  constructor() {
    const ticketId = this.route.snapshot.paramMap.get('ticketId') ?? '';
    this.api.reprint(ticketId).subscribe({
      next: (ticket) => {
        this.ticket.set(ticket);
        this.loading.set(false);
        this.print();
      },
      error: (err: ApiError) => {
        this.error.set(err);
        this.loading.set(false);
      },
    });
  }

  /** Opens the browser print dialog once the ticket has rendered (best-effort; never throws). */
  protected print(): void {
    if (typeof window === 'undefined' || typeof window.print !== 'function') {
      return;
    }
    setTimeout(() => {
      try {
        window.print();
      } catch {
        /* headless/jsdom may not implement print — ignore */
      }
    }, 300);
  }

  protected dateLabel(iso: string): string {
    return new Date(iso).toLocaleString('pt-BR', {
      dateStyle: 'full',
      timeStyle: 'short',
      timeZone: 'America/Sao_Paulo',
    });
  }

  protected errorText(): string {
    return resolveErrorText(this.translate, this.error());
  }
}
