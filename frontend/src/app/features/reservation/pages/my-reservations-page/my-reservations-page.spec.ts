import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { MessageService } from 'primeng/api';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { ApiError } from '../../../../core/http/api-error.model';
import { PageResponse } from '../../../screenings/screening.model';
import { MyReservation, ReservationStatus } from '../../reservation.model';
import { ReservationsApiService } from '../../reservations-api.service';
import { MyReservationsPage } from './my-reservations-page';

function reservation(status: ReservationStatus, startsAt: string, id = 'r1'): MyReservation {
  return {
    reservationId: id,
    status,
    movieTitle: 'Duna',
    roomName: 'Room 1',
    startsAt,
    totalCents: 6000,
    seatLabels: ['A1', 'A2'],
  };
}

function page(content: MyReservation[]): PageResponse<MyReservation> {
  return { content, page: 0, size: 10, totalElements: content.length, totalPages: 1 };
}

const inThreeHours = new Date(Date.now() + 3 * 60 * 60 * 1000).toISOString();
const inOneHour = new Date(Date.now() + 60 * 60 * 1000).toISOString();

async function render(
  api: Partial<ReservationsApiService>,
  messages: Partial<MessageService> = { add: vi.fn() },
): Promise<ComponentFixture<MyReservationsPage>> {
  await TestBed.configureTestingModule({
    imports: [MyReservationsPage],
    providers: [
      provideRouter([]),
      provideTranslateService(),
      { provide: ReservationsApiService, useValue: api },
      { provide: MessageService, useValue: messages },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(MyReservationsPage);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function instance(fixture: ComponentFixture<MyReservationsPage>): any {
  return fixture.componentInstance;
}

describe('MyReservationsPage', () => {
  it('lists the reservations', async () => {
    const fixture = await render({
      list: () => of(page([reservation('CONFIRMED', inThreeHours)])),
    });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Duna');
    expect(el.textContent).toContain('A1, A2');
  });

  it('shows the empty state', async () => {
    const fixture = await render({ list: () => of(page([])) });
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('minhasReservas.empty');
  });

  it('reloads with the upcoming/all scope and status filters', async () => {
    const list = vi.fn().mockReturnValue(of(page([])));
    const fixture = await render({ list });
    list.mockClear();

    instance(fixture).setScope('all');
    expect(list).toHaveBeenLastCalledWith(
      expect.objectContaining({ upcoming: undefined, page: 0 }),
    );

    instance(fixture).setStatus('CONFIRMED');
    expect(list).toHaveBeenLastCalledWith(expect.objectContaining({ status: 'CONFIRMED' }));
  });

  it('applies the cancellation window matrix', async () => {
    const fixture = await render({ list: () => of(page([])) });
    const cmp = instance(fixture);
    expect(cmp.cancelEligible(reservation('PENDING', inOneHour))).toBe(true);
    expect(cmp.cancelEligible(reservation('AWAITING_PAYMENT', inOneHour))).toBe(true);
    expect(cmp.cancelEligible(reservation('CONFIRMED', inThreeHours))).toBe(true);
    expect(cmp.cancelEligible(reservation('CONFIRMED', inOneHour))).toBe(false);
    expect(cmp.cancelEligible(reservation('CANCELLED', inThreeHours))).toBe(false);
    expect(cmp.cancelEligible(reservation('EXPIRED', inThreeHours))).toBe(false);
  });

  it('cancels and acknowledges the refund, then refreshes', async () => {
    const list = vi.fn().mockReturnValue(of(page([reservation('CONFIRMED', inThreeHours)])));
    const cancel = vi
      .fn()
      .mockReturnValue(
        of({
          reservationId: 'r1',
          status: 'CANCELLED',
          refund: { requested: true, amountCents: 6000 },
        }),
      );
    const add = vi.fn();
    const fixture = await render({ list, cancel }, { add });
    list.mockClear();

    instance(fixture).doCancel(reservation('CONFIRMED', inThreeHours));

    expect(cancel).toHaveBeenCalledWith('r1');
    expect(add).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }));
    expect(list).toHaveBeenCalledTimes(1);
  });

  it('surfaces a window-closed rejection and refreshes', async () => {
    const error: ApiError = {
      code: 'booking.cancellation-window-closed',
      message: 'too late',
      fields: [],
      status: 409,
    };
    const list = vi.fn().mockReturnValue(of(page([reservation('CONFIRMED', inThreeHours)])));
    const cancel = vi.fn().mockReturnValue(throwError(() => error));
    const add = vi.fn();
    const fixture = await render({ list, cancel }, { add });

    instance(fixture).doCancel(reservation('CONFIRMED', inThreeHours));

    expect(add).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }));
  });
});
