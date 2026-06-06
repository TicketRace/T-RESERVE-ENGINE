import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { EventItem, EventSession } from '../../models/event';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-event-details',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './event-details.component.html',
  styleUrl: './event-details.component.css',
})
export class EventDetailsComponent implements OnInit {
  event: EventItem | null = null;
  sessions: EventSession[] = [];
  private apiUrl = environment.apiUrl;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly http: HttpClient,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/events']);
      return;
    }
    
    this.http.get<EventItem>(`${this.apiUrl}/api/events/${id}`)
  .subscribe(event => {
    this.event = event;

    this.sessions = [
      {
        id: 1,
        eventId: event.id,
        price: event.basePrice,
        startsAt: event.startTime,
      }
    ];
  });
  }

  selectSession(session: EventSession): void {
    if (!this.event) {
      return;
    }

    this.router.navigate(['/event', this.event.id, 'session', session.id, 'seats']);
  }

  selectSessionByEvent(event: any) {
  this.router.navigate(['/events', event.id, 'sessions']);
}
}
