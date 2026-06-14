import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { PageResponse } from '../screenings/screening.model';
import {
  AdminScreening,
  Movie,
  MovieRequest,
  MovieStatus,
  Room,
  ScreeningRequest,
} from './admin.model';

/** Back-office calls for movies, screenings and rooms (backend SPEC-0008/0009). ADMIN-only. */
@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);

  listMovies(
    query: { status?: MovieStatus; search?: string; page?: number; size?: number } = {},
  ): Observable<PageResponse<Movie>> {
    let params = new HttpParams();
    if (query.status) {
      params = params.set('status', query.status);
    }
    if (query.search) {
      params = params.set('search', query.search);
    }
    params = params.set('page', String(query.page ?? 0)).set('size', String(query.size ?? 20));
    return this.http.get<PageResponse<Movie>>('/api/admin/movies', { params });
  }

  createMovie(request: MovieRequest): Observable<Movie> {
    return this.http.post<Movie>('/api/admin/movies', request);
  }

  updateMovie(id: string, request: MovieRequest): Observable<Movie> {
    return this.http.put<Movie>(`/api/admin/movies/${id}`, request);
  }

  archiveMovie(id: string): Observable<Movie> {
    return this.http.post<Movie>(`/api/admin/movies/${id}/archive`, {});
  }

  unarchiveMovie(id: string): Observable<Movie> {
    return this.http.post<Movie>(`/api/admin/movies/${id}/unarchive`, {});
  }

  listScreenings(
    query: { movieId?: string; roomId?: string; page?: number; size?: number } = {},
  ): Observable<PageResponse<AdminScreening>> {
    let params = new HttpParams();
    if (query.movieId) {
      params = params.set('movieId', query.movieId);
    }
    if (query.roomId) {
      params = params.set('roomId', query.roomId);
    }
    params = params.set('page', String(query.page ?? 0)).set('size', String(query.size ?? 50));
    return this.http.get<PageResponse<AdminScreening>>('/api/admin/screenings', { params });
  }

  createScreening(request: ScreeningRequest): Observable<AdminScreening> {
    return this.http.post<AdminScreening>('/api/admin/screenings', request);
  }

  cancelScreening(id: string): Observable<AdminScreening> {
    return this.http.post<AdminScreening>(`/api/admin/screenings/${id}/cancel`, {});
  }

  listRooms(): Observable<Room[]> {
    return this.http.get<Room[]>('/api/admin/rooms');
  }
}
