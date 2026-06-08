export interface Booking {
  ticketId: number;
  eventTitle: string;
  seatLabel: string;
  status: 'BOOKED' | 'LOCKED' | 'CANCELLED';
  price: number;
  bookedAt: string;
  eventStartTime: string;
}

export interface LockResponse {
  lockId: number;
  expiresAt: string;
}
