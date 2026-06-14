/** Back-office domain types mirroring the backend SPEC-0008/0009 admin contracts. */

export type AgeRating = 'L' | 'A10' | 'A12' | 'A14' | 'A16' | 'A18';
export type MovieStatus = 'ACTIVE' | 'ARCHIVED';
export type ScreeningStatus = 'SCHEDULED' | 'CANCELLED';

export const AGE_RATINGS: AgeRating[] = ['L', 'A10', 'A12', 'A14', 'A16', 'A18'];

/** Admin-facing movie (`MovieResponse`). */
export interface Movie {
  id: string;
  title: string;
  durationMinutes: number;
  synopsis: string | null;
  posterUrl: string | null;
  ageRating: AgeRating;
  status: MovieStatus;
}

/** Create/update payload for a movie (`MovieRequest`). */
export interface MovieRequest {
  title: string;
  durationMinutes: number;
  ageRating: AgeRating;
  synopsis?: string | null;
  posterUrl?: string | null;
}

/** Admin-facing screening (`ScreeningResponse`). */
export interface AdminScreening {
  id: string;
  movieId: string;
  roomId: string;
  startsAt: string;
  endsAt: string;
  basePriceCents: number;
  status: ScreeningStatus;
}

/** Create/update payload for a screening (`ScreeningRequest`); `startsAt` is UTC ISO. */
export interface ScreeningRequest {
  movieId: string;
  roomId: string;
  startsAt: string;
  basePriceCents: number;
}

/** A cinema room (`RoomView`). */
export interface Room {
  id: string;
  name: string;
}
