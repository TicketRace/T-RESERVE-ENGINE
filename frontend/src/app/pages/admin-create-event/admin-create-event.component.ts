import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MockApiService } from '../../services/mock-api.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-admin-create-event',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './admin-create-event.component.html',
  styleUrl: './admin-create-event.component.css',
})
export class AdminCreateEventComponent {
  title = '';
  venues: any[] = [];
  selectedVenueId: number | null = null;
  startsAt = '';
  price = 800;
  created = false;
  eventId: number | null = null;
  isEditMode = false;
  originalSession: any;

  constructor(
    private readonly mockApi: MockApiService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
  ) {}

  createEvent(): void {
    if (!this.selectedVenueId) {
      alert('Выберите площадку');
      return;
    }

    const payload = {
      title: this.title,
      description: '...',
      venueId: Number(this.selectedVenueId),
      startsAt: this.startsAt,
      price: this.price,
    };

    const request$ = this.isEditMode && this.eventId
      ? this.mockApi.updateEvent(this.eventId, {
          title: payload.title,
          sessions: [
            {
              ...this.originalSession,
              startsAt: this.startsAt,
              label: new Date(this.startsAt).toLocaleString('ru-RU'),
              price: this.price,
            }
          ]
        })
      : this.mockApi.createEvent(payload);

    request$.subscribe({
      next: () => {
        this.created = true;
        setTimeout(() => this.router.navigate(['/admin']), 800);
      },
      error: (err) => console.error(err),
    });
  }

  openBuilder(): void {
    this.router.navigate(['/admin/venue-builder']);
  }

  ngOnInit(): void {
    this.mockApi.getVenues().subscribe(v => {
      this.venues = v;
    });

    const id = this.route.snapshot.paramMap.get('id');

    if (id) {
      this.isEditMode = true;
      this.eventId = Number(id);

      this.mockApi.getEventById(this.eventId).subscribe(event => {
        this.title = event.title;
        this.startsAt = event.sessions[0]?.startsAt ?? '';
        this.selectedVenueId = this.venues.find(v => v.name === event.venue)?.id ?? null;
        this.price = event.sessions[0]?.price ?? 0;
      });
    }
  }
}
