import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PageResponse, PublicScreening, SeatMap } from './screening.model';
import { ScreeningsService } from './screenings.service';

describe('ScreeningsService', () => {
  let service: ScreeningsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ScreeningsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists screenings and maps the page', () => {
    let result: PageResponse<PublicScreening> | undefined;
    service.list({ size: 50 }).subscribe((r) => (result = r));

    const req = http.expectOne((r) => r.url === '/api/screenings');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('size')).toBe('50');
    req.flush({
      content: [
        {
          id: 'a1',
          movieTitle: 'Duna',
          ageRating: 'A14',
          posterUrl: null,
          roomName: 'Room 1',
          startsAt: '2026-06-20T20:00:00Z',
          durationMinutes: 120,
          fromPriceCents: 3000,
        },
      ],
      page: 0,
      size: 50,
      totalElements: 1,
      totalPages: 1,
    } satisfies PageResponse<PublicScreening>);

    expect(result?.content.length).toBe(1);
    expect(result?.content[0].movieTitle).toBe('Duna');
  });

  it('passes the date and movieId filters', () => {
    service.list({ date: '2026-06-20', movieId: 'm1' }).subscribe();

    const req = http.expectOne((r) => r.url === '/api/screenings');
    expect(req.request.params.get('date')).toBe('2026-06-20');
    expect(req.request.params.get('movieId')).toBe('m1');
    req.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('fetches a screening seat map', () => {
    let map: SeatMap | undefined;
    service.seatMap('s1').subscribe((m) => (map = m));

    const req = http.expectOne('/api/screenings/s1/seats');
    expect(req.request.method).toBe('GET');
    req.flush({ screeningId: 's1', roomName: 'Room 1', startsAt: 'x', seats: [] } satisfies SeatMap);

    expect(map?.screeningId).toBe('s1');
  });
});
