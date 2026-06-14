/** Stable pagination envelope returned by the backend (shared/pagination). */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** One upcoming session on the public listing (backend SPEC-0010). */
export interface PublicScreening {
  id: string;
  movieTitle: string;
  ageRating: string;
  posterUrl: string | null;
  roomName: string;
  startsAt: string;
  durationMinutes: number;
  fromPriceCents: number;
}

export type SeatStatus = 'FREE' | 'HELD' | 'SOLD';

/** One seat on the screening seat map (backend SPEC-0011). */
export interface SeatMapSeat {
  seatId: string;
  row: string;
  number: number;
  type: string;
  status: SeatStatus;
  fullPriceCents: number;
}

/** The seat map of a screening. */
export interface SeatMap {
  screeningId: string;
  roomName: string;
  startsAt: string;
  seats: SeatMapSeat[];
}
