import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Seat } from '../../models/seat';
import { CommonModule } from '@angular/common';
import { KeyValue } from '@angular/common';

@Component({
  selector: 'app-seat-map',
  standalone: true,
  imports: [CommonModule], 
  templateUrl: './seat-map.component.html',
  styleUrl: './seat-map.component.css',
})
export class SeatMapComponent {
  @Input() groupedSeats: Record<string, Seat[]> = {};
  @Input() selectedSeatId: number | null = null;
  @Output() readonly seatSelected = new EventEmitter<Seat>();

  onSeatClick(seat: Seat): void {
    if (seat.status === 'AVAILABLE') {
      this.seatSelected.emit(seat);
    }
  };
  sortRows = (
    a: KeyValue<string, Seat[]>,
    b: KeyValue<string, Seat[]>
  ): number => {
    return a.key.localeCompare(b.key);
  };
}
