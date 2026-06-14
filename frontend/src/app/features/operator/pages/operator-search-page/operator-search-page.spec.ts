import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { OperatorReservation } from '../../operator.model';
import { OperatorApiService } from '../../operator-api.service';
import { OperatorSearchPage } from './operator-search-page';

const result: OperatorReservation = {
  reservationId: 'res1',
  status: 'CONFIRMED',
  customerName: 'Ana',
  customerEmail: 'ana@example.com',
  movieTitle: 'Duna',
  startsAt: '2026-06-14T23:31:00.000Z',
  seatLabels: ['A2'],
};

async function render(
  api: Partial<OperatorApiService>,
): Promise<ComponentFixture<OperatorSearchPage>> {
  await TestBed.configureTestingModule({
    imports: [OperatorSearchPage],
    providers: [
      provideRouter([]),
      provideTranslateService(),
      { provide: OperatorApiService, useValue: api },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(OperatorSearchPage);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function instance(fixture: ComponentFixture<OperatorSearchPage>): any {
  return fixture.componentInstance;
}

describe('OperatorSearchPage', () => {
  it('searches with the selected criterion and renders results', async () => {
    const search = vi.fn().mockReturnValue(of([result]));
    const fixture = await render({ search });

    instance(fixture).setCriterion('ticketCode');
    instance(fixture).form.setValue({ query: 'FKM-2026-000003' });
    instance(fixture).search();
    fixture.detectChanges();

    expect(search).toHaveBeenCalledWith('ticketCode', 'FKM-2026-000003');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Ana');
  });

  it('does not search on an empty query', async () => {
    const search = vi.fn();
    const fixture = await render({ search });
    instance(fixture).form.setValue({ query: '   ' });
    instance(fixture).search();
    expect(search).not.toHaveBeenCalled();
  });

  it('runs the search on form submit via (ngSubmit) — regression: no native page reload', async () => {
    // The form must carry a FormGroup so submitting fires (ngSubmit) (which preventDefaults)
    // instead of doing a native GET submit that reloads the page and drops the session.
    const search = vi.fn().mockReturnValue(of([result]));
    const fixture = await render({ search });
    instance(fixture).form.setValue({ query: 'FKM-2026-000003' });

    const form = (fixture.nativeElement as HTMLElement).querySelector('form')!;
    const event = new Event('submit', { cancelable: true });
    form.dispatchEvent(event);

    expect(search).toHaveBeenCalledWith('ticketCode', 'FKM-2026-000003');
    expect(event.defaultPrevented).toBe(true);
  });

  it('loads detail for a reservation', async () => {
    const detail = vi
      .fn()
      .mockReturnValue(
        of({ reservation: { tickets: [] }, customerName: 'Ana', customerEmail: 'ana@example.com' }),
      );
    const fixture = await render({ search: () => of([result]), detail });
    instance(fixture).openDetail(result);
    expect(detail).toHaveBeenCalledWith('res1');
  });
});
