import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { LockResponse } from '../models/booking';
import { MockApiService } from './mock-api.service';

@Injectable({
  providedIn: 'root',
})
export class BookingService {
  constructor(private readonly mockApi: MockApiService) {}

  lockSeat(sessionId: number, seatId: number): Observable<LockResponse> {
    return this.mockApi.lockSeat(sessionId, seatId);
  }

  confirmBooking(lockId: number, paymentMethod: string): Observable<string> {
    return this.mockApi.confirmBooking(lockId, paymentMethod);
  }

  cancelBooking(lockId: number): Observable<void> {
    return this.mockApi.cancelBooking(lockId);
  }
}