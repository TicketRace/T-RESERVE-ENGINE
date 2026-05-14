import { Component, EventEmitter, Input, Output } from '@angular/core';
import { EventItem } from '../../models/event';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-event-card',
  standalone: true,
  imports: [CommonModule], 
  templateUrl: './event-card.component.html',
  styleUrl: './event-card.component.css',
})
export class EventCardComponent {
  @Input({ required: true }) event!: EventItem;
  @Output() readonly open = new EventEmitter<number>();

  openEvent(): void {
    this.open.emit(this.event.id);
  }
}
