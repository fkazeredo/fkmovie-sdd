import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  OperatorReservation,
  OperatorReservationDetail,
  Reprint,
  SearchCriterion,
} from './operator.model';

/** Operator reservation lookup and ticket reprint (backend SPEC-0020). OPERATOR/ADMIN only. */
@Injectable({ providedIn: 'root' })
export class OperatorApiService {
  private readonly http = inject(HttpClient);

  /** Searches by exactly one criterion (the others are never sent). */
  search(criterion: SearchCriterion, value: string): Observable<OperatorReservation[]> {
    const params = new HttpParams().set(criterion, value);
    return this.http.get<OperatorReservation[]>('/api/operator/reservations', { params });
  }

  detail(reservationId: string): Observable<OperatorReservationDetail> {
    return this.http.get<OperatorReservationDetail>(`/api/operator/reservations/${reservationId}`);
  }

  reprint(ticketId: string): Observable<Reprint> {
    return this.http.post<Reprint>(`/api/operator/tickets/${ticketId}/reprint`, {});
  }
}
