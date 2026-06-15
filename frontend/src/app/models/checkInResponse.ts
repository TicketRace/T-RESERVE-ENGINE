export interface CheckInResponse {
  message?: string;
  status: string;
  ticketId: number;
  eventId?: number;
  eventName?: string;
  seatLabel?: string;
  checkedInAt?: string;
}
