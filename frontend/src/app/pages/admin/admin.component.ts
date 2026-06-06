import { Component, OnInit } from '@angular/core';
import { AdminEventSummary } from '../../models/event';
import { User } from '../../models/user';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, RouterLink], 
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css',
})
export class AdminComponent implements OnInit {
  user: User | null = null;
  events: AdminEventSummary[] = [];
  showCreatedModal = false;
  private apiUrl = environment.apiUrl;

  constructor(
    private readonly authService: AuthService,
    private readonly http: HttpClient,
  ) {}

  loadEvents(): void {
    this.http.get<any>(`${this.apiUrl}/api/events`)
      .subscribe(res => {
        this.events = (res.content ?? res).map((e: any) => ({
          id: e.id,
          title: e.title,
          venue: e.venue?.name,
          nextSession: e.startTime,
        }));
      });
  }

  delete(id: number): void {
    const confirmed = confirm('Вы действительно хотите удалить это событие?');

    if (!confirmed) return;

    this.http.delete(
      `${this.apiUrl}/api/admin/events/${id}`
    ).subscribe(() => this.loadEvents());
  }

  ngOnInit(): void {
    this.user = this.authService.snapshot();
    this.loadEvents();
  }
}
