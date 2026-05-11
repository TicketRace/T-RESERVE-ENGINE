import { Injectable } from '@angular/core';
import { LockResponse } from '../models/booking';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
export class BookingService {
  constructor(
    private readonly http: HttpClient
  ) {}
 lockSeat(eventId: number, seatId: number) {
    return this.http.post<LockResponse>(
      'http://localhost:8080/api/bookings/lock',
      { eventId, seatId }
    );
  }

  confirmBooking(lockId: number) {
    return this.http.post(
      `http://localhost:8080/api/bookings/${lockId}/confirm`,
      {},
      { responseType: 'text' }
    );
  }

  cancelBooking(lockId: number) {
    return this.http.delete(
      `http://localhost:8080/api/bookings/${lockId}`
    );
  }
}