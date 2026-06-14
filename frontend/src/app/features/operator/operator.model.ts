import { Reservation, ReservationStatus } from '../reservation/reservation.model';

/** The criterion an operator searches by — exactly one is sent (backend SPEC-0020). */
export type SearchCriterion = 'ticketCode' | 'reservationId' | 'email';

/** One reservation in an operator search result (`OperatorReservationView`). */
export interface OperatorReservation {
  reservationId: string;
  status: ReservationStatus;
  customerName: string;
  customerEmail: string;
  movieTitle: string;
  startsAt: string;
  seatLabels: string[];
}

/** Full operator detail of a reservation (`OperatorReservationDetailView`). */
export interface OperatorReservationDetail {
  reservation: Reservation;
  customerName: string;
  customerEmail: string;
}

/** Printable ticket payload returned by a reprint (`ReprintView`). */
export interface Reprint {
  ticketCode: string;
  seatLabel: string;
  movieTitle: string;
  roomName: string;
  startsAt: string;
  customerName: string;
  reprintCount: number;
}
