import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, interval, switchMap, takeUntil } from 'rxjs';
import { EventItem, EventSession } from '../../models/event';
import { Seat } from '../../models/seat';
import { MockApiService } from '../../services/mock-api.service';
import { CommonModule } from '@angular/common';
import { SeatMapComponent } from '../../components/seat-map/seat-map.component';

@Component({
  selector: 'app-seat-selection',
  standalone: true,
  imports: [CommonModule, SeatMapComponent],
  templateUrl: './seat-selection.component.html',
  styleUrl: './seat-selection.component.css',
})
export class SeatSelectionComponent implements OnInit, OnDestroy {
  event: EventItem | null = null;
  session: EventSession | null = null;
  seats: Seat[] = [];
  selectedSeat: Seat | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly mockApi: MockApiService,
  ) {}

  ngOnInit(): void {
    const eventId = Number(this.route.snapshot.paramMap.get('id'));
    const sessionId = Number(this.route.snapshot.paramMap.get('sessionId'));

    this.mockApi.getEventById(eventId).subscribe((event) => {
      this.event = event;
    });

    this.mockApi.getSession(sessionId).subscribe((session) => {
      this.session = session;
    });

    this.loadSeats(sessionId);
    interval(3000)
      .pipe(
        takeUntil(this.destroy$),
        switchMap(() => this.mockApi.getSeats(sessionId)),
      )
      .subscribe((seats) => {
        this.seats = seats;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  selectSeat(seat: Seat): void {
    this.selectedSeat = seat;
  }

  proceed(): void {
    if (!this.selectedSeat || !this.session) {
      return;
    }

    this.router.navigate(['/payment', this.session.id], {
      state: {
        selectedSeat: this.selectedSeat,
        event: this.event,
        session: this.session,
      },
    });
  }

  private loadSeats(sessionId: number): void {
    this.mockApi.getSeats(sessionId).subscribe((seats) => {
      this.seats = seats;
    });
  }
}
