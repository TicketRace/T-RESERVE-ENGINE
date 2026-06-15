import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TicketService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  downloadPdf(ticketId: number) {
    return this.http.get(
      `${this.apiUrl}/api/tickets/${ticketId}/download`,
      { responseType: 'blob' }
    );
  }
}
