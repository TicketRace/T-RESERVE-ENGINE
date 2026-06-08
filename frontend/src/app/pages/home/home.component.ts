import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { EventItem } from '../../models/event';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EventCardComponent } from '../../components/event-card/event-card.component';
import { HeroComponent } from '../../components/hero/hero.component';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, EventCardComponent, FormsModule, HeroComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
  static cachedEvents: EventItem[] | null = null;
  events: EventItem[] = [];
  filteredEvents: EventItem[] = [];
  loading = true;
  search = '';
  selectedCategory = '';
  notification: string | null = null;
  private apiUrl = environment.apiUrl;

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
  ) { }

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
    this.filteredEvents = this.events.filter(e => {
      const matchesSearch = e.title.toLowerCase().includes(term) ||
        e.description?.toLowerCase().includes(term) ||
        e.venue?.name?.toLowerCase().includes(term);

      const matchesCategory = this.selectedCategory === '' || e.category === this.selectedCategory;

      return matchesSearch && matchesCategory;
    });
  }

  selectCategory(category: string): void {
    this.selectedCategory = category;
    this.onSearch();
  }

  trackEventId(index: number, event: EventItem): number {
    return event.id;
  }

  scrollToEvents(): void {
    const el = document.getElementById('events-catalog');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  openEvent(id: number): void {
    this.router.navigate(['/event', id]);
  }

  private fetchEvents(): void {
    if (HomeComponent.cachedEvents) {
      this.events = HomeComponent.cachedEvents;
      this.filteredEvents = HomeComponent.cachedEvents;
      this.loading = false;
      this.onSearch();
      return;
    }

    this.http.get<any>(`${this.apiUrl}/api/events?page=0&size=20`)
      .subscribe({
        next: (res) => {
          this.events = res.content;
          this.filteredEvents = res.content;
          HomeComponent.cachedEvents = res.content;
          this.loading = false;
        },
        error: () => this.loading = false,
      });
  }
}
