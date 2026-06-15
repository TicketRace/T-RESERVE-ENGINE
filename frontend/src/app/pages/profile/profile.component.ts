import { Component, OnInit } from '@angular/core';
import { Booking } from '../../models/booking';
import { User } from '../../models/user';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { TicketService } from '../../services/ticket.service';
import { TicketPublicResponse } from '../../models/ticket-public-response';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
})
export class ProfileComponent implements OnInit {
  user: User | null = null;
  bookings: Booking[] = [];
  private apiUrl = environment.apiUrl;
  rawPdfUrl: string | null = null;
  selectedPdfUrl: SafeResourceUrl | null = null;
  showPdfModal = false;

  constructor(
  private readonly http: HttpClient,
  private readonly ticketService: TicketService,
  private readonly sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.http.get<User>(`${this.apiUrl}/api/users/me`)
      .subscribe(user => this.user = user);

    this.http.get<Booking[]>(`${this.apiUrl}/api/users/me/bookings`)
      .subscribe(bookings => {
        this.bookings = bookings.filter(
          booking => booking.status === 'BOOKED'
        );
      });
  }

  qrUrls: Record<number, string> = {};
  activeQrTicketId: number | null = null;
  activeQrUrl: string | null = null;

  openQrModal(ticketId: number): void {
    if (this.qrUrls[ticketId]) {
      this.activeQrTicketId = ticketId;
      this.activeQrUrl = this.qrUrls[ticketId];
      return;
    }

    this.http.get(`${this.apiUrl}/api/tickets/${ticketId}/qr`, { responseType: 'blob' })
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          this.qrUrls[ticketId] = url;
          this.activeQrTicketId = ticketId;
          this.activeQrUrl = url;
        },
        error: (err) => {
          console.error('Failed to load QR code', err);
        }
      });
  }

  closeQrModal(): void {
    this.activeQrTicketId = null;
    this.activeQrUrl = null;
  }

  downloadTicketPDF(ticketId: number): void {
    this.ticketService.downloadPdf(ticketId).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);

      const a = document.createElement('a');
      a.href = url;
      a.download = `ticket-${ticketId}.pdf`;
      a.click();

      window.URL.revokeObjectURL(url);
    });
  }



  closePdf(): void {
    this.showPdfModal = false;

    if (this.rawPdfUrl) {
      window.URL.revokeObjectURL(this.rawPdfUrl);
    }

    this.rawPdfUrl = null;
    this.selectedPdfUrl = null;
  }
}
