import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AdminApiService } from './admin-api.service';

describe('AdminApiService', () => {
  let service: AdminApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AdminApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists movies with status + search', () => {
    service.listMovies({ status: 'ACTIVE', search: 'duna' }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/admin/movies');
    expect(req.request.params.get('status')).toBe('ACTIVE');
    expect(req.request.params.get('search')).toBe('duna');
    req.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('creates a movie', () => {
    service.createMovie({ title: 'Duna', durationMinutes: 166, ageRating: 'A14' }).subscribe();
    const req = http.expectOne('/api/admin/movies');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ title: 'Duna', durationMinutes: 166, ageRating: 'A14' });
    req.flush({});
  });

  it('archives and unarchives a movie', () => {
    service.archiveMovie('m1').subscribe();
    http.expectOne('/api/admin/movies/m1/archive').flush({});
    service.unarchiveMovie('m1').subscribe();
    http.expectOne('/api/admin/movies/m1/unarchive').flush({});
  });

  it('creates a screening with a UTC instant', () => {
    service
      .createScreening({
        movieId: 'm1',
        roomId: 'r1',
        startsAt: '2026-06-15T23:30:00.000Z',
        basePriceCents: 3000,
      })
      .subscribe();
    const req = http.expectOne('/api/admin/screenings');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      movieId: 'm1',
      roomId: 'r1',
      startsAt: '2026-06-15T23:30:00.000Z',
      basePriceCents: 3000,
    });
    req.flush({});
  });

  it('lists rooms', () => {
    service.listRooms().subscribe();
    const req = http.expectOne('/api/admin/rooms');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 'r1', name: 'Room 1' }]);
  });
});
