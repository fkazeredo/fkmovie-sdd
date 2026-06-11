import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CORRELATION_ID_HEADER, correlationIdInterceptor } from './correlation-id.interceptor';

describe('correlationIdInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([correlationIdInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('adds the X-Correlation-Id header to every request', () => {
    http.get('/api/anything').subscribe();

    const request = controller.expectOne('/api/anything');
    const header = request.request.headers.get(CORRELATION_ID_HEADER);
    expect(header).toBeTruthy();
    expect(header).toMatch(/^[0-9a-f-]{36}$/);
    request.flush({});
  });

  it('reuses the same correlation id within the session', () => {
    http.get('/api/one').subscribe();
    http.get('/api/two').subscribe();

    const first = controller.expectOne('/api/one').request.headers.get(CORRELATION_ID_HEADER);
    const second = controller.expectOne('/api/two').request.headers.get(CORRELATION_ID_HEADER);
    expect(first).toBe(second);
    controller.expectNone('/api/none');
  });
});
