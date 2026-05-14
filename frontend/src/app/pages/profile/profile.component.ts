import { Component, OnInit } from '@angular/core';
import { Booking } from '../../models/booking';
import { User } from '../../models/user';
import { MockApiService } from '../../services/mock-api.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule], 
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
})
export class ProfileComponent implements OnInit {
  user: User | null = null;
  bookings: Booking[] = [];

  constructor(private readonly mockApi: MockApiService) {}

  ngOnInit(): void {
    this.mockApi.getCurrentUser().subscribe((user) => {
      this.user = user;
    });

    this.mockApi.getUserBookings().subscribe((bookings) => {
      this.bookings = bookings;
    });
  }
}
