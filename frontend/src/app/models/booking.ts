export interface Booking {
  ticketId: number;
  eventId: number;
  eventTitle: string;
  seatId: number;
  seatLabel: string;
  status: 'BOOKED' | 'LOCKED' | 'CANCELLED';
  price: number;
  bookedAt?: string;
  eventStartTime: string;
  lockExpiresAt?: string;
}

export interface LockResponse {
  lockId: number;
  expiresAt: string;
}
