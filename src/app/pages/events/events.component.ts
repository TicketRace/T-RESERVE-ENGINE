import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { EventItem } from '../../models/event';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EventCardComponent } from '../../components/event-card/event-card.component';
import { HttpClient } from '@angular/common/http';

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
    private readonly http: HttpClient,
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
    const term = this.search.toLowerCase().trim();
    this.filteredEvents = this.events.filter(e =>
      e.title.toLowerCase().includes(term) ||
      e.description?.toLowerCase().includes(term)
    );
  }

  openEvent(id: number): void {
    this.router.navigate(['/event', id]);
  }

  private fetchEvents(): void {
    this.http.get<any>('http://localhost:8080/api/events?page=0&size=20')
      .subscribe({
      next: (res) => {
        this.events = res.content;
        this.filteredEvents = res.content;
        this.loading = false;
      },
        error: () => this.loading = false,
      });
  }
}
