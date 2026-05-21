import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';

import { BookingService } from './booking.service';
import { environment } from '../../environments/environment';

describe('BookingService', () => {
  let service: BookingService;
  let httpMock: HttpTestingController;
  const apiUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [BookingService],
    });

    service = TestBed.inject(BookingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  //lock seat
  it('should send lock request', () => {
    service.lockSeat(1, 15).subscribe();

    const req = httpMock.expectOne(
      `${apiUrl}/api/bookings/lock`
    );

    expect(req.request.method).toBe('POST');

    expect(req.request.body).toEqual({
      eventId: 1,
      seatId: 15,
    });

    req.flush({
      lockId: 99,
      expiresAt: '2026-01-01T12:00:00',
    });
  });

  //confirm booking
  it('should confirm booking', () => {
    service.confirmBooking(55).subscribe();

    const req = httpMock.expectOne(
      `${apiUrl}/api/bookings/55/confirm`
    );

    expect(req.request.method).toBe('POST');

    expect(req.request.responseType).toBe('text');

    req.flush('OK');
  });

  //cancel booking
  it('should cancel booking', () => {
    service.cancelBooking(77).subscribe();

    const req = httpMock.expectOne(
      `${apiUrl}/api/bookings/77`
    );

    expect(req.request.method).toBe('DELETE');

    req.flush({});
  });
});