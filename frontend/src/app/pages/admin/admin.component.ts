import { Component, OnInit } from '@angular/core';
import { AdminEventSummary } from '../../models/event';
import { User } from '../../models/user';
import { AuthService } from '../../services/auth.service';
import { MockApiService } from '../../services/mock-api.service';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

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

  constructor(
    private readonly authService: AuthService,
    private readonly mockApi: MockApiService,
  ) {}

  loadEvents(): void {
    this.mockApi.getAdminEvents().subscribe(events => {
      this.events = events;
    });
  }

  delete(id: number): void {
    const confirmed = confirm('Вы действительно хотите удалить это событие?');

    if (!confirmed) return;

    this.mockApi.deleteEvent(id).subscribe(() => {
      this.loadEvents();
    });
  }


  ngOnInit(): void {
    this.user = this.authService.snapshot();
    this.loadEvents();
  }
}
