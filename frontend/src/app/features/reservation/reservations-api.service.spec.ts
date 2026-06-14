import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { Reservation, ReservationSnapshot } from './reservation.model';
import { ReservationsApiService, serverOffsetFrom } from './reservations-api.service';

const reservation = {
  reservationId: 'r1',
  screeningId: 's1',
  status: 'PENDING',
  movieTitle: 'Duna',
  roomName: 'Room 1',
  startsAt: '2026-06-20T20:00:00Z',
  expiresAt: '2026-06-20T19:00:00Z',
  paymentDeadlineAt: null,
  totalCents: 6000,
  seats: [],
  tickets: [],
  refund: null,
} as Reservation;

describe('serverOffsetFrom', () => {
  afterEach(() => vi.restoreAllMocks());

  it('computes the offset from the Date header relative to the client clock', () => {
    vi.spyOn(Date, 'now').mockReturnValue(1000);
    expect(serverOffsetFrom(new Date(5000).toUTCString())).toBe(4000);
  });

  it('returns 0 when the header is missing or unparseable', () => {
    expect(serverOffsetFrom(null)).toBe(0);
    expect(serverOffsetFrom('not-a-date')).toBe(0);
  });
});

describe('ReservationsApiService', () => {
  let service: ReservationsApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReservationsApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('creates a reservation for the chosen seats', () => {
    service.create('s1', [{ seatId: 'a1', ticketType: 'FULL' }]).subscribe();
    const req = http.expectOne('/api/screenings/s1/reservations');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ seats: [{ seatId: 'a1', ticketType: 'FULL' }] });
    req.flush(reservation);
  });

  it('reads a reservation together with the server clock offset', () => {
    let snapshot: ReservationSnapshot | undefined;
    service.get('r1').subscribe((s) => (snapshot = s));
    const req = http.expectOne('/api/reservations/r1');
    expect(req.request.method).toBe('GET');
    req.flush(reservation, { headers: { Date: new Date(Date.now() + 5000).toUTCString() } });

    expect(snapshot?.reservation.reservationId).toBe('r1');
    expect(snapshot?.serverOffsetMs).toBeGreaterThan(1000);
  });

  it('confirms a reservation', () => {
    service.confirm('r1').subscribe();
    const req = http.expectOne('/api/reservations/r1/confirm');
    expect(req.request.method).toBe('POST');
    req.flush({
      reservationId: 'r1',
      status: 'AWAITING_PAYMENT',
      paymentId: 'p1',
      paymentDeadlineAt: 'x',
    });
  });

  it('cancels a reservation', () => {
    service.cancel('r1').subscribe();
    const req = http.expectOne('/api/reservations/r1/cancel');
    expect(req.request.method).toBe('POST');
    req.flush({
      reservationId: 'r1',
      status: 'CANCELLED',
      refund: { requested: false, amountCents: 0 },
    });
  });
});
