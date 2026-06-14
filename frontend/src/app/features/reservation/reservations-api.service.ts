import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import {
  CancellationResult,
  ConfirmationResult,
  CreateReservationSeat,
  Reservation,
  ReservationSnapshot,
} from './reservation.model';

/** Estimates the client→server clock offset (ms) from a response `Date` header; 0 when absent. */
export function serverOffsetFrom(dateHeader: string | null): number {
  if (!dateHeader) {
    return 0;
  }
  const serverMs = Date.parse(dateHeader);
  return Number.isNaN(serverMs) ? 0 : serverMs - Date.now();
}

/** Reservation lifecycle calls (backend SPEC-0014/0016/0018). All require an authenticated owner. */
@Injectable({ providedIn: 'root' })
export class ReservationsApiService {
  private readonly http = inject(HttpClient);

  create(screeningId: string, seats: CreateReservationSeat[]): Observable<Reservation> {
    return this.http.post<Reservation>(`/api/screenings/${screeningId}/reservations`, { seats });
  }

  /** Fetches a reservation plus the server clock offset (for a skew-resistant countdown). */
  get(id: string): Observable<ReservationSnapshot> {
    return this.http.get<Reservation>(`/api/reservations/${id}`, { observe: 'response' }).pipe(
      map((response) => ({
        reservation: response.body as Reservation,
        serverOffsetMs: serverOffsetFrom(response.headers.get('Date')),
      })),
    );
  }

  confirm(id: string): Observable<ConfirmationResult> {
    return this.http.post<ConfirmationResult>(`/api/reservations/${id}/confirm`, {});
  }

  cancel(id: string): Observable<CancellationResult> {
    return this.http.post<CancellationResult>(`/api/reservations/${id}/cancel`, {});
  }
}
