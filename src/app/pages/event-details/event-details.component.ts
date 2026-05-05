import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { EventItem, EventSession } from '../../models/event';
import { MockApiService } from '../../services/mock-api.service';
import { CommonModule } from '@angular/common';

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

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly mockApi: MockApiService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/events']);
      return;
    }

    this.mockApi.getEventById(id).subscribe((event) => {
      this.event = event;
    });

    this.mockApi.getSessions(id).subscribe((sessions) => {
      this.sessions = sessions;
    });
  }

  selectSession(session: EventSession): void {
    if (!this.event) {
      return;
    }

    this.router.navigate(['/event', this.event.id, 'session', session.id, 'seats']);
  }
}
