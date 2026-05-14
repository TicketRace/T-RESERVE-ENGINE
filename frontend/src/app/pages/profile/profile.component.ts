import { Component, OnInit } from '@angular/core';
import { Booking } from '../../models/booking';
import { User } from '../../models/user';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

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

  constructor(
  private readonly http: HttpClient,
  ) {}

  ngOnInit(): void {
  this.http.get<User>('http://localhost:8080/api/users/me')
    .subscribe(user => this.user = user);

  this.http.get<Booking[]>('http://localhost:8080/api/users/me/bookings')
    .subscribe(bookings => {
    this.bookings = bookings.filter(
      booking => booking.status === 'BOOKED'
    );
  });
  }
}
