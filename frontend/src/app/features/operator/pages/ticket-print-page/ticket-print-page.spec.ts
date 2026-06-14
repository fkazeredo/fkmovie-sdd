import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { Reprint } from '../../operator.model';
import { OperatorApiService } from '../../operator-api.service';
import { TicketPrintPage } from './ticket-print-page';

const reprint: Reprint = {
  ticketCode: 'FKM-2026-000003',
  seatLabel: 'A2',
  movieTitle: 'Duna Parte Dois',
  roomName: 'Room 1',
  startsAt: '2026-06-14T23:31:00.000Z',
  customerName: 'Cliente E2E',
  reprintCount: 1,
};

async function render(
  api: Partial<OperatorApiService>,
): Promise<ComponentFixture<TicketPrintPage>> {
  await TestBed.configureTestingModule({
    imports: [TicketPrintPage],
    providers: [
      provideRouter([]),
      provideTranslateService(),
      { provide: OperatorApiService, useValue: api },
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 't1' } } } },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(TicketPrintPage);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture;
}

describe('TicketPrintPage', () => {
  it('reprints the ticket on load and renders its data', async () => {
    const reprintFn = vi.fn().mockReturnValue(of(reprint));
    // window.print is a jsdom noop; spy so the print trigger does not warn
    vi.spyOn(window, 'print').mockImplementation(() => undefined);

    const fixture = await render({ reprint: reprintFn });

    expect(reprintFn).toHaveBeenCalledWith('t1');
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('FKM-2026-000003');
    expect(text).toContain('Duna Parte Dois');
    expect(text).toContain('A2');
  });
});
