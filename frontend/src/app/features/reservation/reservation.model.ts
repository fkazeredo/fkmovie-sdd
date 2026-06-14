/** Reservation domain types mirroring the backend SPEC-0014/0016/0018/0019 contracts. */

export type ReservationStatus =
  | 'PENDING'
  | 'AWAITING_PAYMENT'
  | 'CONFIRMED'
  | 'CANCELLED'
  | 'EXPIRED';
export type TicketType = 'FULL' | 'HALF';

/** One seat of a reservation with display info and the snapshotted price (`ReservationSeatView`). */
export interface ReservationSeat {
  seatId: string;
  row: string;
  number: number;
  type: string;
  ticketType: TicketType;
  priceCents: number;
}

/** One issued ticket (present once CONFIRMED) (`TicketView`). */
export interface Ticket {
  ticketId: string;
  code: string;
  seatLabel: string;
  status: string;
}

/** Refund summary, present only on a cancelled CONFIRMED reservation (`RefundSummary`). */
export interface RefundSummary {
  amountCents: number;
  status: string;
}

/** The full reservation read model (`ReservationView`). */
export interface Reservation {
  reservationId: string;
  screeningId: string;
  status: ReservationStatus;
  movieTitle: string;
  roomName: string;
  startsAt: string;
  expiresAt: string | null;
  paymentDeadlineAt: string | null;
  totalCents: number;
  seats: ReservationSeat[];
  tickets: Ticket[];
  refund: RefundSummary | null;
}

/** One requested seat in a create-reservation call (`SeatSelectionRequest`). */
export interface CreateReservationSeat {
  seatId: string;
  ticketType: TicketType;
  halfPriceCategory?: string;
  documentReference?: string;
}

/** Response of POST /api/reservations/{id}/confirm (`ReservationConfirmationView`). */
export interface ConfirmationResult {
  reservationId: string;
  status: ReservationStatus;
  paymentId: string;
  paymentDeadlineAt: string;
}

/** Response of POST /api/reservations/{id}/cancel (`CancellationView`). */
export interface CancellationResult {
  reservationId: string;
  status: ReservationStatus;
  refund: { requested: boolean; amountCents: number };
}

/** A reservation fetched together with the estimated client→server clock offset (ms). */
export interface ReservationSnapshot {
  reservation: Reservation;
  serverOffsetMs: number;
}

/** One row of the customer's reservation history (`MyReservationView`, backend SPEC-0019). */
export interface MyReservation {
  reservationId: string;
  status: ReservationStatus;
  movieTitle: string;
  roomName: string;
  startsAt: string;
  totalCents: number;
  seatLabels: string[];
}

/** Query for the customer's reservation history (backend SPEC-0019). */
export interface MyReservationsQuery {
  status?: ReservationStatus;
  upcoming?: boolean;
  page?: number;
  size?: number;
}
