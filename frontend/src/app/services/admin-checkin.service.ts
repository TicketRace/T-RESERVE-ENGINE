import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { CheckInResponse } from '../models/models/checkInResponse';

@Injectable({ providedIn: 'root' })
export class AdminCheckInService {

  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  checkInById(ticketId: number) {
    return this.http.post<CheckInResponse>(
      `${this.apiUrl}/api/admin/tickets/check-in/by-id/${ticketId}`,
      {}
    );
  }

  checkInByToken(token: string) {
    return this.http.post<CheckInResponse>(
      `${this.apiUrl}/api/admin/tickets/check-in`,
      { token }
    );
  }
}
