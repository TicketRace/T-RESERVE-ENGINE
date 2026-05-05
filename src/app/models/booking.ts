import { EventItem, EventSession } from './event';

export interface Booking {
  ticketId: number;
  event: EventItem;
  session: EventSession;
  seat: string;
  status: 'BOOKED' | 'LOCKED' | 'CANCELLED';
  bookedAt: string;
  paymentMethod: string;
}

export interface LockResponse {
  lockId: number;
  expiresAt: string;
}
