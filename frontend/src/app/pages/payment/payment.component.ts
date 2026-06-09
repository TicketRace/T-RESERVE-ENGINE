import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { EventSession } from '../../models/event';
import { Seat } from '../../models/seat';
import { BookingService } from '../../services/booking.service';
import { CommonModule } from '@angular/common';
import { lastValueFrom } from 'rxjs';

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment.component.html',
  styleUrl: './payment.component.css',
})
export class PaymentComponent implements OnInit, OnDestroy {
  session: EventSession | null = null;
  selectedSeats: Seat[] = [];

  lockedSeats: { seat: Seat; lockId: number; expiresAt: string }[] = [];
  failedSeats: Seat[] = [];

  expiresLabel = '10:00';

  private timerInterval: any;
  private readonly LOCK_KEY = 'payment_locks';

  errorMessage: string | null = null;
  isLoading = false;

  showConflictDialog = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly bookingService: BookingService,
  ) {}

  ngOnInit(): void {
    const sessionId = Number(this.route.snapshot.paramMap.get('sessionId'));

    const state = history.state as {
      selectedSeats?: Seat[];
      session?: EventSession;
    };

    this.selectedSeats = state?.selectedSeats ?? [];
    this.session = (state?.session ?? (state as any)?.event) ?? null;

    if (this.selectedSeats.length === 0) {
      const saved = sessionStorage.getItem('pending_seat_selection');
      if (saved) {
        try {
          const parsed = JSON.parse(saved);
          this.selectedSeats = parsed.selectedSeats || (parsed.selectedSeat ? [parsed.selectedSeat] : []);
          this.session = parsed.event || parsed.session;
          sessionStorage.removeItem('pending_seat_selection');
        } catch (e) {
          console.error('Failed to parse pending seat selection', e);
        }
      }
    }

    if (this.selectedSeats.length === 0) {
      this.router.navigate(['/']);
      return;
    }

    const savedLocks = sessionStorage.getItem(this.LOCK_KEY);
    if (savedLocks) {
      const parsed = JSON.parse(savedLocks);
      this.lockedSeats = parsed.lockedSeats;
      if (this.lockedSeats.length > 0) {
        this.startTimer(this.lockedSeats[0].expiresAt);
      }
    } else {
      this.createLocks(sessionId);
    }
  }

  private async createLocks(sessionId: number): Promise<void> {
    this.isLoading = true;
    this.errorMessage = null;

    for (const seat of this.selectedSeats) {
      try {
        const lock = await lastValueFrom(this.bookingService.lockSeat(sessionId, seat.seatId));
        this.lockedSeats.push({ seat, lockId: lock.lockId, expiresAt: lock.expiresAt });
      } catch (err: any) {
        this.failedSeats.push(seat);
      }
    }

    this.isLoading = false;

    if (this.failedSeats.length > 0) {
      if (this.lockedSeats.length === 0) {
        this.errorMessage = 'Все выбранные места уже заняты.';
        setTimeout(() => this.goBack(), 2000);
      } else {
        this.showConflictDialog = true;
      }
    } else {
      this.saveLocksAndStartTimer();
    }
  }

  private saveLocksAndStartTimer(): void {
    sessionStorage.setItem(this.LOCK_KEY, JSON.stringify({ lockedSeats: this.lockedSeats }));
    if (this.lockedSeats.length > 0) {
      this.startTimer(this.lockedSeats[0].expiresAt);
    }
  }

  acceptPartialBooking(): void {
    this.showConflictDialog = false;
    this.selectedSeats = this.lockedSeats.map(l => l.seat);
    this.saveLocksAndStartTimer();
  }

  async cancelPartialBooking(): Promise<void> {
    this.showConflictDialog = false;
    this.isLoading = true;
    for (const lock of this.lockedSeats) {
      try {
        await lastValueFrom(this.bookingService.cancelBooking(lock.lockId));
      } catch (e) {
        console.error('Failed to cancel lock', e);
      }
    }
    this.isLoading = false;
    this.goBack();
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

  async payFree(): Promise<void> {
    if (this.lockedSeats.length === 0) return;

    this.isLoading = true;
    this.errorMessage = null;

    for (const lock of this.lockedSeats) {
      try {
        await lastValueFrom(this.bookingService.confirmBooking(lock.lockId));
      } catch (err) {
        this.errorMessage = 'Ошибка оплаты одного из билетов.';
        this.isLoading = false;
        return;
      }
    }

    this.isLoading = false;
    sessionStorage.removeItem(this.LOCK_KEY);
    this.router.navigate(['/payment-success']);
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

  getTotalPrice(): number {
    return this.selectedSeats.reduce((sum, seat) => sum + seat.price, 0);
  }
}
