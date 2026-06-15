import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, interval, switchMap, takeUntil } from 'rxjs';
import { EventItem } from '../../models/event';
import { Seat } from '../../models/seat';
import { CommonModule } from '@angular/common';
import { SeatMapComponent } from '../../components/seat-map/seat-map.component';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../services/auth.service';
import { WebSocketService } from '../../services/websocket.service';

@Component({
  selector: 'app-seat-selection',
  standalone: true,
  imports: [CommonModule, SeatMapComponent, RouterLink],
  templateUrl: './seat-selection.component.html',
  styleUrl: './seat-selection.component.css',
})
export class SeatSelectionComponent implements OnInit, OnDestroy {
  event: EventItem | null = null;
  seats: Seat[] = [];
  selectedSeatIds: Set<number> = new Set();
  selectedSeats: Seat[] = [];
  private readonly destroy$ = new Subject<void>();
  groupedSeats: Record<string, Seat[]> = {};
  private apiUrl = environment.apiUrl;

  private updateGroupedSeats(): void {
    const map: Record<string, Seat[]> = {};

    for (const seat of this.seats) {
      if (!map[seat.rowLabel]) {
        map[seat.rowLabel] = [];
      }
      map[seat.rowLabel].push(seat);
    }

    Object.keys(map).forEach(row => {
      map[row].sort((a, b) => a.seatNumber - b.seatNumber);
    });

    const sortedRows = Object.keys(map).sort();
    const sortedMap: Record<string, Seat[]> = {};

    for (const row of sortedRows) {
      sortedMap[row] = map[row];
    }

    this.groupedSeats = sortedMap;
  }

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly http: HttpClient,
    private readonly authService: AuthService,
    private readonly ws: WebSocketService
  ) {}

  ngOnInit(): void {
    const eventId = Number(this.route.snapshot.paramMap.get('id'));

    console.log('eventId:', eventId);

    this.http
      .get<EventItem>(`${this.apiUrl}/api/events/${eventId}`)
      .subscribe(event => {
        this.event = event;
        console.log('EVENT:', event);
      });

    this.http
      .get<Seat[]>(`${this.apiUrl}/api/events/${eventId}/seats`)
      .subscribe({
        next: seats => {
          console.log('SEATS FIRST LOAD:', seats);
          this.seats = seats;
          this.updateGroupedSeats();
        },
        error: err => console.error('SEATS ERROR:', err)
      });

    this.ws.connect(eventId, (seats) => {
      console.log('WS UPDATE:', seats);
      this.seats = seats;
      this.updateGroupedSeats();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.ws.disconnect();
  }

  private readonly MAX_SEATS = 5;

  selectSeat(seat: Seat): void {
    if (this.selectedSeatIds.has(seat.seatId)) {
      this.selectedSeatIds.delete(seat.seatId);
      this.selectedSeats = this.selectedSeats.filter(s => s.seatId !== seat.seatId);
    } else {
      if (this.selectedSeatIds.size >= this.MAX_SEATS) {
        alert(`Максимум ${this.MAX_SEATS} билетов на один заказ`);
        return;
      }
      this.selectedSeatIds.add(seat.seatId);
      this.selectedSeats.push(seat);
    }
  }

  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  getTotalPrice(): number {
    return this.selectedSeats.reduce((sum, seat) => sum + seat.price, 0);
  }

  proceed(): void {
    if (this.selectedSeats.length === 0) return;

    if (this.isLoggedIn) {
      this.router.navigate(['/payment', this.event?.id], {
        state: {
          selectedSeats: this.selectedSeats,
          event: this.event,
        },
      });
    } else {
      // Save pending seat selection before login redirect
      sessionStorage.setItem('pending_seat_selection', JSON.stringify({
        selectedSeats: this.selectedSeats,
        event: this.event,
      }));

      this.router.navigate(['/login'], {
        queryParams: { 
          returnUrl: `/payment/${this.event?.id}`,
          message: 'Для продолжения бронирования и покупки билетов, пожалуйста, авторизуйтесь.'
        }
      });
    }
  }
  
}
