import { Injectable } from '@angular/core';
import { LockResponse } from '../models/booking';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class BookingService {
  private apiUrl = environment.apiUrl;
  constructor(
    private readonly http: HttpClient
  ) {}
 lockSeat(eventId: number, seatId: number) {
    return this.http.post<LockResponse>(
      `${this.apiUrl}/api/bookings/lock`,
      { eventId, seatId }
    );
  }

  confirmBooking(lockId: number) {
    return this.http.post(
      `${this.apiUrl}/api/bookings/${lockId}/confirm`,
      {},
      { responseType: 'text' }
    );
  }

  cancelBooking(lockId: number) {
    return this.http.delete(
      `${this.apiUrl}/api/bookings/${lockId}`
    );
  }
}
