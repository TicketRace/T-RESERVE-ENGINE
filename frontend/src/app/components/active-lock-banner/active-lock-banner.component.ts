import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Booking } from '../../models/booking';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-active-lock-banner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="active-lock-banner animate-fade-in" *ngIf="lockedBookings.length > 0">
      <div class="banner-content">
        <span class="banner-icon">!</span>
        <div class="banner-text">
          <span class="banner-title">У вас есть незавершенное бронирование: <strong>{{ lockedBookings[0].eventTitle }}</strong></span>
          <span class="banner-subtitle">Места забронированы за вами еще {{ expiresLabel }}</span>
        </div>
      </div>
      <button class="action-chip resume-action" (click)="resumePayment()">
        Продолжить оформление
      </button>
    </div>
  `,
  styles: [`
    .active-lock-banner {
      position: fixed;
      bottom: 30px;
      left: 30px;
      z-index: 10000;
      background: linear-gradient(135deg, rgba(207, 168, 86, 0.95) 0%, rgba(188, 160, 92, 0.9) 100%);
      color: #111;
      padding: 20px 24px;
      border-radius: 20px;
      display: flex;
      flex-direction: column;
      gap: 16px;
      align-items: flex-start;
      backdrop-filter: blur(12px);
      border: 1px solid rgba(255, 255, 255, 0.2);
      box-shadow: 0 10px 40px rgba(0, 0, 0, 0.4);
      max-width: 380px;
      animation: slideUpFade 0.4s cubic-bezier(0.16, 1, 0.3, 1) forwards;
    }
    @keyframes slideUpFade {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }
    .banner-content {
      display: flex;
      align-items: flex-start;
      gap: 16px;
    }
    .banner-icon {
      font-size: 1.5rem;
      font-weight: 800;
      margin-top: 2px;
    }
    .banner-text {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .banner-title {
      font-size: 0.95rem;
      font-weight: 600;
      line-height: 1.4;
    }
    .banner-subtitle {
      font-size: 0.85rem;
      opacity: 0.85;
    }
    .resume-action {
      width: 100%;
      background: #111;
      color: #fff;
      border: none;
      padding: 12px 16px;
      border-radius: 12px;
      font-weight: 600;
      font-size: 0.95rem;
      cursor: pointer;
      transition: all 0.2s;
      text-align: center;
    }
    .resume-action:hover {
      background: #333;
      transform: translateY(-1px);
    }
    @media (max-width: 768px) {
      .active-lock-banner {
        bottom: 20px;
        right: 20px;
        left: 20px;
        max-width: none;
      }
    }
  `]
})
export class ActiveLockBannerComponent implements OnInit, OnDestroy {
  lockedBookings: Booking[] = [];
  expiresLabel = '00:00';
  private timerInterval?: any;
  private readonly LOCK_KEY = 'payment_locks';

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    // Check if user is authenticated (simple check: if token exists or just try fetching)
    this.http.get<Booking[]>(`${environment.apiUrl}/api/users/me/bookings`).subscribe({
      next: (bookings) => {
        this.lockedBookings = bookings.filter(b => b.status === 'LOCKED');
        if (this.lockedBookings.length > 0 && this.lockedBookings[0].lockExpiresAt) {
          // Verify that it's not already in sessionStorage on the payment page
          const isOnPaymentPage = this.router.url.includes('/payment/');
          if (!isOnPaymentPage) {
            this.startTimer(this.lockedBookings[0].lockExpiresAt);
          } else {
            this.lockedBookings = []; // Don't show banner if already on payment page
          }
        }
      },
      error: () => {
        // User not logged in, ignore
      }
    });

    // Also listen to router events to hide banner if we navigate to payment page
    this.router.events.subscribe(() => {
      if (this.router.url.includes('/payment/')) {
        this.lockedBookings = [];
        this.stopTimer();
      }
    });
  }

  ngOnDestroy(): void {
    this.stopTimer();
  }

  private startTimer(expiresAt: string): void {
    const expires = new Date(expiresAt).getTime();

    const tick = () => {
      const remaining = expires - Date.now();

      if (remaining <= 0) {
        this.expiresLabel = '00:00';
        this.stopTimer();
        this.lockedBookings = [];
        return;
      }

      const sec = Math.floor(remaining / 1000);
      const m = Math.floor(sec / 60);
      const s = sec % 60;

      this.expiresLabel = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    };

    tick();
    this.timerInterval = setInterval(tick, 1000);
  }

  private stopTimer(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  resumePayment(): void {
    if (this.lockedBookings.length === 0) return;

    // Transform to what PaymentComponent expects
    const lockedSeatsObj = this.lockedBookings.map(b => ({
      seat: { seatId: b.seatId, seatLabel: b.seatLabel, price: b.price },
      lockId: b.ticketId,
      expiresAt: b.lockExpiresAt
    }));

    sessionStorage.setItem(this.LOCK_KEY, JSON.stringify({ lockedSeats: lockedSeatsObj }));
    const eventId = this.lockedBookings[0].eventId;
    
    const selectedSeats = this.lockedBookings.map(b => ({
      seatId: b.seatId,
      seatLabel: b.seatLabel,
      price: b.price
    }));

    const session = {
      id: eventId,
      title: this.lockedBookings[0].eventTitle,
      startsAt: this.lockedBookings[0].eventStartTime
    };

    this.lockedBookings = [];
    this.stopTimer();

    this.router.navigate(['/payment', eventId], {
      state: { selectedSeats, session }
    });
  }
}
