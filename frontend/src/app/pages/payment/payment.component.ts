import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { EventSession } from '../../models/event';
import { Seat } from '../../models/seat';
import { BookingService } from '../../services/booking.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment.component.html',
  styleUrl: './payment.component.css',
})
export class PaymentComponent implements OnInit, OnDestroy {
  session: EventSession | null = null;
  selectedSeat: Seat | null = null;

  lockId: number | null = null;
  expiresLabel = '10:00';

  private timerInterval: any;
  private readonly LOCK_KEY = 'payment_lock';

  errorMessage: string | null = null;
  isLoading = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly bookingService: BookingService,
  ) {}

  ngOnInit(): void {
    const sessionId = Number(this.route.snapshot.paramMap.get('sessionId'));

    const state = history.state as {
      selectedSeat?: Seat;
      session?: EventSession;
    };

    this.selectedSeat = state?.selectedSeat ?? null;
    this.session = (state?.session ?? (state as any)?.event) ?? null;

    // Restore pending seat selection if we were redirected from the login page
    if (!this.selectedSeat) {
      const saved = sessionStorage.getItem('pending_seat_selection');
      if (saved) {
        try {
          const parsed = JSON.parse(saved);
          this.selectedSeat = parsed.selectedSeat;
          this.session = parsed.event || parsed.session;
          sessionStorage.removeItem('pending_seat_selection');
        } catch (e) {
          console.error('Failed to parse pending seat selection', e);
        }
      }
    }

    if (!this.selectedSeat) {
      this.router.navigate(['/']);
      return;
    }

    const saved = sessionStorage.getItem(this.LOCK_KEY);

    if (saved) {
      const parsed = JSON.parse(saved);
      this.lockId = parsed.lockId;
      this.startTimer(parsed.expiresAt);
    } else {
      this.createLock(sessionId);
    }
  }

  private createLock(sessionId: number): void {
    if (!this.selectedSeat) return;

    this.isLoading = true;
    this.errorMessage = null;

    this.bookingService
      .lockSeat(sessionId, this.selectedSeat.seatId)
      .subscribe({
        next: (lock) => {
          this.isLoading = false;

          this.lockId = lock.lockId;
          sessionStorage.setItem(this.LOCK_KEY, JSON.stringify(lock));

          this.startTimer(lock.expiresAt);
        },
        error: (err) => {
          this.isLoading = false;

          if (err.status === 409) {
            this.errorMessage = 'Место уже занято';
            setTimeout(() => this.router.navigate(['/']), 2000);
          } else {
            this.errorMessage = 'Ошибка бронирования';
          }
        },
      });
  }

  private startTimer(expiresAt: string): void {
    const expires = new Date(expiresAt).getTime();

    const tick = () => {
      const remaining = expires - Date.now();

      if (remaining <= 0) {
        this.expiresLabel = '00:00';
        clearInterval(this.timerInterval);
        sessionStorage.removeItem(this.LOCK_KEY);

        this.router.navigate(['/'], {
          state: { message: 'Время бронирования истекло' },
        });
        return;
      }

      const sec = Math.floor(remaining / 1000);
      const m = Math.floor(sec / 60);
      const s = sec % 60;

      this.expiresLabel =
        `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    };

    tick();
    this.timerInterval = setInterval(tick, 1000);
  }

  payFree(): void {
    if (!this.lockId) return;

    this.bookingService.confirmBooking(this.lockId).subscribe({
      next: () => {
        sessionStorage.removeItem(this.LOCK_KEY);
        this.router.navigate(['/payment-success']);
      },
      error: () => {
        this.errorMessage = 'Ошибка оплаты';
      },
    });
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }

    sessionStorage.removeItem(this.LOCK_KEY);
  }

  goBack(): void {
    window.history.back();

    this.router.navigate(
      ['/event', this.session?.id],
      { state: { reloadSeats: true } }
    );
  }
}
