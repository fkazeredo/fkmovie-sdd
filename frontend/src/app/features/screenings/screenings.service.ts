import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { PageResponse, PublicScreening, SeatMap } from './screening.model';

/** Reads the public screenings list and a screening's seat map (backend SPEC-0010 / 0011). */
@Injectable({ providedIn: 'root' })
export class ScreeningsService {
  private readonly http = inject(HttpClient);

  list(
    options: { date?: string; movieId?: string; page?: number; size?: number } = {},
  ): Observable<PageResponse<PublicScreening>> {
    let params = new HttpParams();
    if (options.date) {
      params = params.set('date', options.date);
    }
    if (options.movieId) {
      params = params.set('movieId', options.movieId);
    }
    if (options.page != null) {
      params = params.set('page', String(options.page));
    }
    if (options.size != null) {
      params = params.set('size', String(options.size));
    }
    return this.http.get<PageResponse<PublicScreening>>('/api/screenings', { params });
  }

  seatMap(screeningId: string): Observable<SeatMap> {
    return this.http.get<SeatMap>(`/api/screenings/${screeningId}/seats`);
  }
}
