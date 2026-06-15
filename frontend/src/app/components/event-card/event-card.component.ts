import { Component, EventEmitter, Input, Output, ElementRef, NgZone, OnInit, OnDestroy } from '@angular/core';
import { EventItem } from '../../models/event';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-event-card',
  standalone: true,
  imports: [CommonModule], 
  templateUrl: './event-card.component.html',
  styleUrl: './event-card.component.css',
})
export class EventCardComponent implements OnInit, OnDestroy {
  @Input({ required: true }) event!: EventItem;
  @Output() readonly open = new EventEmitter<number>();

  constructor(private el: ElementRef, private zone: NgZone) {}

  ngOnInit() {
    // Bind touch events OUTSIDE Angular zone so it doesn't trigger change detection or hang the UI
    this.zone.runOutsideAngular(() => {
      this.el.nativeElement.addEventListener('touchstart', this.onTouchStart, { passive: true });
      this.el.nativeElement.addEventListener('touchend', this.onTouchEnd, { passive: true });
      this.el.nativeElement.addEventListener('touchcancel', this.onTouchEnd, { passive: true });
    });
  }

  ngOnDestroy() {
    this.el.nativeElement.removeEventListener('touchstart', this.onTouchStart);
    this.el.nativeElement.removeEventListener('touchend', this.onTouchEnd);
    this.el.nativeElement.removeEventListener('touchcancel', this.onTouchEnd);
  }

  private onTouchStart = () => {
    this.el.nativeElement.classList.add('is-pressed');
  };

  private onTouchEnd = () => {
    setTimeout(() => {
      this.el.nativeElement.classList.remove('is-pressed');
    }, 50); // slight delay to ensure visual feedback is seen on extremely fast taps
  };

  openEvent(): void {
    this.open.emit(this.event.id);
  }
}
