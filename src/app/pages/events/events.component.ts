import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { EventItem } from '../../models/event';
import { MockApiService } from '../../services/mock-api.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EventCardComponent } from '../../components/event-card/event-card.component';

@Component({
  selector: 'app-events',
  standalone: true,
  imports: [CommonModule, EventCardComponent, FormsModule],
  templateUrl: './events.component.html',
  styleUrl: './events.component.css',
})
export class EventsComponent implements OnInit {
  events: EventItem[] = [];
  filteredEvents: EventItem[] = [];
  loading = true;
  search = '';
  notification: string | null = null;
  

  constructor(
    private readonly mockApi: MockApiService,
    private readonly router: Router,
  ) {}

  
  ngOnInit(): void {
    this.fetchEvents();
    
    const navigation = this.router.getCurrentNavigation();
    const state = navigation?.extras.state as { message?: string }
      || history.state;

    if (state?.message) {
      this.notification = state.message;

      setTimeout(() => {
        this.notification = null;
      }, 3000);
    }
  }

  onSearch(): void {
    this.fetchEvents(this.search);
  }

  openEvent(id: number): void {
    this.router.navigate(['/event', id]);
  }

  private fetchEvents(search = ''): void {
    this.mockApi.getEvents(search).subscribe({
      next: (events) => {
        this.events = events;
        this.filteredEvents = events;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }
}
