export interface TicketPublicResponse {
  ticketId: number;
  status: 'BOOKED' | 'USED';
  eventId: number;
  eventTitle: string;
  eventDescription: string;
  eventStartTime: string;
  venueName: string;
  venueAddress: string;
  rowLabel: string;
  seatNumber: number;
  seatLabel: string;
  price: number;
  customerName: string;
}
