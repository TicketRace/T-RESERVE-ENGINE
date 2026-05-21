import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-admin-create-event',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './admin-create-event.component.html',
  styleUrl: './admin-create-event.component.css',
})
export class AdminCreateEventComponent {
  title = '';
  description = '';
  imageUrl = '';
  category = '';
  ageRestriction = '';
  durationMinutes = 120;

  venues: any[] = [];
  selectedVenueId: number | null = null;

  startTime = '';
  basePrice = 800;

  created = false;

  eventId: number | null = null;
  isEditMode = false;

  private apiUrl = environment.apiUrl;

  constructor(
    private readonly http: HttpClient,
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
      description: this.description,
      venueId: Number(this.selectedVenueId),

      startTime: new Date(this.startTime).toISOString(),
      basePrice: this.basePrice,

      imageUrl: this.imageUrl,
      ageRestriction: this.ageRestriction,
      category: this.category,
      durationMinutes: Number(this.durationMinutes),
    };

    const updatePayload = {
    title: this.title,
    description: this.description,
    status: 'ACTIVE',

    startTime: new Date(this.startTime).toISOString(),
    basePrice: this.basePrice,

    imageUrl: this.imageUrl,
    ageRestriction: this.ageRestriction,
    category: this.category,
    durationMinutes: Number(this.durationMinutes),
  };

    const request$ = this.isEditMode && this.eventId
      ? this.http.put(
          `${this.apiUrl}/api/admin/events/${this.eventId}`,
          updatePayload
        )
      : this.http.post(
          `${this.apiUrl}/api/admin/events`,
          payload
        );

    request$.subscribe({
      next: () => {
        this.created = true;

        setTimeout(() => {
          this.router.navigate(['/admin']);
        }, 800);
      },

      error: (err) => {
        console.error(err);
        alert('Ошибка сохранения');
      },
    });
  }

  openBuilder(): void {
    this.router.navigate(['/admin/venue-builder']);
  }

  goBack(): void {
    this.router.navigate(['/admin']);
  }

  ngOnInit(): void {
    
   this.http.get<any[]>(`${this.apiUrl}/api/venues`)
    .subscribe(v => this.venues = v);

  const id = this.route.snapshot.paramMap.get('id');

  if (id) {
    this.isEditMode = true;
    this.eventId = Number(id);

    this.http.get<any>(`${this.apiUrl}/api/events/${this.eventId}`)
      .subscribe(event => {

        this.title = event.title;
        this.description = event.description;

        this.imageUrl = event.imageUrl;

        this.category = event.category;
        this.ageRestriction = event.ageRestriction;

        this.durationMinutes = event.durationMinutes;

        this.startTime = event.startTime?.slice(0, 16);

        this.basePrice = event.basePrice;

        this.selectedVenueId = event.venue?.id;
      });
    }
  }
}
