import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AuthService } from '../../../../core/auth/auth.service';
import { ApiError } from '../../../../core/http/api-error.model';
import { Reservation } from '../../../reservation/reservation.model';
import { ReservationsApiService } from '../../../reservation/reservations-api.service';
import { SeatMap } from '../../screening.model';
import { ScreeningsService } from '../../screenings.service';
import { SeatMapPage } from './seat-map-page';

const map: SeatMap = {
  screeningId: 's1',
  roomName: 'Room 1',
  startsAt: '2026-06-20T20:00:00Z',
  seats: [
    { seatId: 'a1', row: 'A', number: 1, type: 'STANDARD', status: 'FREE', fullPriceCents: 3000 },
    { seatId: 'a2', row: 'A', number: 2, type: 'STANDARD', status: 'SOLD', fullPriceCents: 3000 },
    { seatId: 'b1', row: 'B', number: 1, type: 'VIP', status: 'FREE', fullPriceCents: 4000 },
  ],
};

const reservation = { reservationId: 'r1', screeningId: 's1' } as Reservation;

function authStub(authenticated: boolean, verified: boolean): Partial<AuthService> {
  return {
    isAuthenticated: () => authenticated,
    emailVerified: () => verified,
  } as unknown as Partial<AuthService>;
}

async function render(
  screenings: Partial<ScreeningsService>,
  auth: Partial<AuthService> = authStub(false, false),
  reservations: Partial<ReservationsApiService> = {},
): Promise<ComponentFixture<SeatMapPage>> {
  await TestBed.configureTestingModule({
    imports: [SeatMapPage],
    providers: [
      provideRouter([]),
      provideTranslateService(),
      { provide: ScreeningsService, useValue: screenings as ScreeningsService },
      { provide: AuthService, useValue: auth },
      { provide: ReservationsApiService, useValue: reservations as ReservationsApiService },
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 's1' } } } },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(SeatMapPage);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function instance(fixture: ComponentFixture<SeatMapPage>): any {
  return fixture.componentInstance;
}

describe('SeatMapPage', () => {
  it('renders the seats grouped by row', async () => {
    const fixture = await render({ seatMap: () => of(map) });
    const buttons = (fixture.nativeElement as HTMLElement).querySelectorAll('button[title]');
    expect(buttons.length).toBe(3);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Room 1');
  });

  it('disables sold seats and selects free seats on click', async () => {
    const fixture = await render({ seatMap: () => of(map) });
    const el = fixture.nativeElement as HTMLElement;
    const seatButtons = Array.from(el.querySelectorAll<HTMLButtonElement>('button[title]'));
    const sold = seatButtons.find((b) => b.title.startsWith('A2'));
    const free = seatButtons.find((b) => b.title.startsWith('A1'));
    expect(sold?.disabled).toBe(true);
    expect(free?.disabled).toBe(false);

    free?.click();
    fixture.detectChanges();
    expect(el.textContent).toContain('1');
  });

  it('redirects an anonymous user to /login when reserving', async () => {
    const fixture = await render({ seatMap: () => of(map) }, authStub(false, false));
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    instance(fixture).selected.set(['a1']);
    instance(fixture).reserve();
    expect(navigate).toHaveBeenCalledWith(
      ['/login'],
      expect.objectContaining({ queryParams: expect.anything() }),
    );
  });

  it('reserves the selected seats and navigates to the reservation for a verified user', async () => {
    const create = vi.fn().mockReturnValue(of(reservation));
    const fixture = await render({ seatMap: () => of(map) }, authStub(true, true), { create });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    instance(fixture).selected.set(['a1', 'b1']);
    instance(fixture).reserve();

    expect(create).toHaveBeenCalledWith('s1', [
      { seatId: 'a1', ticketType: 'FULL' },
      { seatId: 'b1', ticketType: 'FULL' },
    ]);
    expect(navigate).toHaveBeenCalledWith(['/reservas', 'r1']);
  });

  it('highlights stolen seats on a seats-unavailable rejection', async () => {
    const error: ApiError = {
      code: 'booking.seats-unavailable',
      message: 'taken',
      fields: [{ field: 'seatId', message: 'a1' }],
      status: 409,
    };
    const seatMap = vi.fn().mockReturnValue(of(map));
    const create = vi.fn().mockReturnValue(throwError(() => error));
    const fixture = await render({ seatMap }, authStub(true, true), { create });

    instance(fixture).selected.set(['a1']);
    instance(fixture).reserve();
    fixture.detectChanges();

    expect(instance(fixture).unavailable()).toEqual(['a1']);
    // selection cleared and the map refetched to reflect the new statuses
    expect(instance(fixture).selected()).toEqual([]);
    expect(seatMap).toHaveBeenCalledTimes(2);
  });
});
