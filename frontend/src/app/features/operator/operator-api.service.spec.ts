import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { OperatorApiService } from './operator-api.service';

describe('OperatorApiService', () => {
  let service: OperatorApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OperatorApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('searches by exactly one criterion (others are not sent)', () => {
    service.search('ticketCode', 'FKM-2026-000003').subscribe();
    const req = http.expectOne((r) => r.url === '/api/operator/reservations');
    expect(req.request.params.get('ticketCode')).toBe('FKM-2026-000003');
    expect(req.request.params.has('email')).toBe(false);
    expect(req.request.params.has('reservationId')).toBe(false);
    req.flush([]);
  });

  it('searches by email exclusively', () => {
    service.search('email', 'a@b.com').subscribe();
    const req = http.expectOne((r) => r.url === '/api/operator/reservations');
    expect(req.request.params.get('email')).toBe('a@b.com');
    expect(req.request.params.has('ticketCode')).toBe(false);
    req.flush([]);
  });

  it('fetches reservation detail', () => {
    service.detail('res1').subscribe();
    const req = http.expectOne('/api/operator/reservations/res1');
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('reprints a ticket', () => {
    service.reprint('t1').subscribe();
    const req = http.expectOne('/api/operator/tickets/t1/reprint');
    expect(req.request.method).toBe('POST');
    req.flush({});
  });
});
