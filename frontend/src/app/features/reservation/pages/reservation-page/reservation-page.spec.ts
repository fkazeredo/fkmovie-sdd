import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { Observable, of } from 'rxjs';
import { vi } from 'vitest';

import { AuthService } from '../../../../core/auth/auth.service';
import { Reservation, ReservationSnapshot, ReservationStatus } from '../../reservation.model';
import { ReservationsApiService } from '../../reservations-api.service';
import { ReservationPage } from './reservation-page';

function reservation(status: ReservationStatus, overrides: Partial<Reservation> = {}): Reservation {
  return {
    reservationId: 'r1',
    screeningId: 's1',
    status,
    movieTitle: 'Duna',
    roomName: 'Room 1',
    startsAt: '2026-06-20T20:00:00Z',
    expiresAt: status === 'PENDING' ? new Date(Date.now() + 600_000).toISOString() : null,
    paymentDeadlineAt:
      status === 'AWAITING_PAYMENT' ? new Date(Date.now() + 120_000).toISOString() : null,
    totalCents: 6000,
    seats: [
      { seatId: 'a1', row: 'A', number: 1, type: 'STANDARD', ticketType: 'FULL', priceCents: 6000 },
    ],
    tickets:
      status === 'CONFIRMED'
        ? [{ ticketId: 't1', code: 'ABC123', seatLabel: 'A1', status: 'VALID' }]
        : [],
    refund: status === 'CANCELLED' ? { amountCents: 6000, status: 'REQUESTED' } : null,
    ...overrides,
  };
}

function snap(
  status: ReservationStatus,
  overrides: Partial<Reservation> = {},
): ReservationSnapshot {
  return { reservation: reservation(status, overrides), serverOffsetMs: 0 };
}

const verified = {
  isAuthenticated: () => true,
  emailVerified: () => true,
} as unknown as Partial<AuthService>;

async function render(
  get: (id: string) => Observable<ReservationSnapshot>,
  api: Partial<ReservationsApiService> = {},
): Promise<ComponentFixture<ReservationPage>> {
  await TestBed.configureTestingModule({
    imports: [ReservationPage],
    providers: [
      provideRouter([]),
      provideTranslateService(),
      { provide: ReservationsApiService, useValue: { get, ...api } },
      { provide: AuthService, useValue: verified },
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'r1' } } } },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(ReservationPage);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function instance(fixture: ComponentFixture<ReservationPage>): any {
  return fixture.componentInstance;
}

function text(fixture: ComponentFixture<ReservationPage>): string {
  return (fixture.nativeElement as HTMLElement).textContent ?? '';
}

describe('ReservationPage', () => {
  it('renders PENDING with a countdown and confirm/cancel actions', async () => {
    const fixture = await render(() => of(snap('PENDING')));
    expect(text(fixture)).toContain('reservation.confirm');
    expect(text(fixture)).toContain('reservation.cancel');
    expect(text(fixture)).toMatch(/\d\d:\d\d/);
  });

  it('renders AWAITING_PAYMENT as a processing state', async () => {
    const fixture = await render(() => of(snap('AWAITING_PAYMENT')));
    expect(text(fixture)).toContain('reservation.processing');
  });

  it('renders CONFIRMED with the issued tickets', async () => {
    const fixture = await render(() => of(snap('CONFIRMED')));
    expect(text(fixture)).toContain('ABC123');
    expect(text(fixture)).toContain('A1');
  });

  it('renders CANCELLED with the refund amount', async () => {
    const fixture = await render(() => of(snap('CANCELLED')));
    expect(text(fixture)).toContain('reservation.cancelledTitle');
    expect(text(fixture)).toContain('R$');
  });

  it('renders EXPIRED with a path back to the seat map', async () => {
    const fixture = await render(() => of(snap('EXPIRED')));
    expect(text(fixture)).toContain('reservation.expiredTitle');
  });

  it('computes a server-anchored countdown', async () => {
    const fixture = await render(() => of(snap('PENDING')));
    const cmp = instance(fixture);
    cmp.serverOffsetMs.set(0);
    cmp.nowMs.set(5000);
    cmp.reservation.set(reservation('PENDING', { expiresAt: new Date(65_000).toISOString() }));
    expect(cmp.countdownLabel()).toBe('01:00');

    // server 2 s ahead of the client → 2 s less remaining
    cmp.serverOffsetMs.set(2000);
    expect(cmp.countdownLabel()).toBe('00:58');
  });

  it('confirms the reservation and moves into the awaiting-payment state', async () => {
    const get = vi
      .fn()
      .mockReturnValueOnce(of(snap('PENDING')))
      .mockReturnValue(of(snap('AWAITING_PAYMENT')));
    const confirm = vi
      .fn()
      .mockReturnValue(of({ reservationId: 'r1', status: 'AWAITING_PAYMENT' }));
    const fixture = await render(get, { confirm });

    instance(fixture).confirm();

    expect(confirm).toHaveBeenCalledWith('r1');
    expect(instance(fixture).reservation()?.status).toBe('AWAITING_PAYMENT');
  });

  it('cancels the reservation', async () => {
    const get = vi
      .fn()
      .mockReturnValueOnce(of(snap('PENDING')))
      .mockReturnValue(of(snap('CANCELLED')));
    const cancel = vi.fn().mockReturnValue(of({ reservationId: 'r1', status: 'CANCELLED' }));
    const fixture = await render(get, { cancel });

    instance(fixture).cancel();

    expect(cancel).toHaveBeenCalledWith('r1');
    expect(instance(fixture).reservation()?.status).toBe('CANCELLED');
  });

  it('polls the reservation until the payment confirms', async () => {
    const get = vi
      .fn()
      .mockReturnValueOnce(of(snap('AWAITING_PAYMENT')))
      .mockReturnValue(of(snap('CONFIRMED')));
    await TestBed.configureTestingModule({
      imports: [ReservationPage],
      providers: [
        provideRouter([]),
        provideTranslateService(),
        { provide: ReservationsApiService, useValue: { get } },
        { provide: AuthService, useValue: verified },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'r1' } } } },
      ],
    }).compileComponents();

    vi.useFakeTimers();
    try {
      const fixture = TestBed.createComponent(ReservationPage);
      fixture.detectChanges();
      expect(instance(fixture).reservation()?.status).toBe('AWAITING_PAYMENT');

      await vi.advanceTimersByTimeAsync(2000);
      fixture.detectChanges();

      expect(instance(fixture).reservation()?.status).toBe('CONFIRMED');
    } finally {
      vi.useRealTimers();
    }
  });
});
