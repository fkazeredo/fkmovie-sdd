import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { apiErrorInterceptor } from './api-error.interceptor';
import { ApiError } from './api-error.model';

describe('apiErrorInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('maps the backend {code, message, fields} payload to a typed ApiError', () => {
    let captured: ApiError | undefined;
    http.get('/api/unknown').subscribe({ error: (error: ApiError) => (captured = error) });

    controller.expectOne('/api/unknown').flush(
      {
        code: 'not-found',
        message: 'O recurso solicitado não foi encontrado.',
        fields: [{ field: 'id', message: 'invalid' }],
      },
      { status: 404, statusText: 'Not Found' },
    );

    expect(captured).toBeDefined();
    expect(captured?.code).toBe('not-found');
    expect(captured?.message).toBe('O recurso solicitado não foi encontrado.');
    expect(captured?.fields).toEqual([{ field: 'id', message: 'invalid' }]);
    expect(captured?.status).toBe(404);
  });

  it('normalizes non-backend failures to network.error', () => {
    let captured: ApiError | undefined;
    http.get('/api/down').subscribe({ error: (error: ApiError) => (captured = error) });

    controller
      .expectOne('/api/down')
      .flush('<html>Bad Gateway</html>', { status: 502, statusText: 'Bad Gateway' });

    expect(captured?.code).toBe('network.error');
    expect(captured?.fields).toEqual([]);
    expect(captured?.status).toBe(502);
  });
});
