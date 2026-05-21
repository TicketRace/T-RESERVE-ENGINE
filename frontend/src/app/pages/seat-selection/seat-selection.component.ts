import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, interval, switchMap, takeUntil } from 'rxjs';
import { EventItem } from '../../models/event';
import { Seat } from '../../models/seat';
import { CommonModule } from '@angular/common';
import { SeatMapComponent } from '../../components/seat-map/seat-map.component';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-seat-selection',
  standalone: true,
  imports: [CommonModule, SeatMapComponent],
  templateUrl: './seat-selection.component.html',
  styleUrl: './seat-selection.component.css',
})
export class SeatSelectionComponent implements OnInit, OnDestroy {
  event: EventItem | null = null;
  seats: Seat[] = [];
  selectedSeat: Seat | null = null;
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

    interval(3000)
      .pipe(
        takeUntil(this.destroy$),
        switchMap(() =>
          this.http.get<Seat[]>(`${this.apiUrl}/api/events/${eventId}/seats`)
        )
      )
      .subscribe(seats => {
        console.log('SEATS UPDATE:', seats);
        this.seats = seats;
        this.updateGroupedSeats();
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
    if (!this.selectedSeat) return;

    this.router.navigate(['/payment', this.event?.id], {
      state: {
        selectedSeat: this.selectedSeat,
        event: this.event,
      },
    });
  }
  
}
