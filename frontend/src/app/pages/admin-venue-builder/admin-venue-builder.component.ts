import { Component, OnInit } from '@angular/core';
import { VenueSeatTemplate } from '../../models/event';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-admin-venue-builder',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-venue-builder.component.html',
  styleUrl: './admin-venue-builder.component.css',
})
export class AdminVenueBuilderComponent implements OnInit {
  venue = 'Площадка';
  seats: VenueSeatTemplate[] = [];

  ngOnInit(): void {
    this.seats = [];
  }

  toggleSeat(target: VenueSeatTemplate): void {
    target.disabled = !target.disabled;
  }
}
