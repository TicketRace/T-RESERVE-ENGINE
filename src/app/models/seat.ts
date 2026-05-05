export type SeatStatus = 'AVAILABLE' | 'LOCKED' | 'BOOKED';

export interface Seat {
  seatId: number;
  seatLabel: string;
  rowLabel: string;
  seatNumber: number;
  status: SeatStatus;
  price: number;
}
