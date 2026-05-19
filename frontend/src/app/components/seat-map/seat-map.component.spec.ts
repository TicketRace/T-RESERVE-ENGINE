import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SeatMapComponent } from './seat-map.component';
import { Seat } from '../../models/seat';

describe('SeatMapComponent', () => {
  let component: SeatMapComponent;
  let fixture: ComponentFixture<SeatMapComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SeatMapComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SeatMapComponent);
    component = fixture.componentInstance;
  });

  it('should emit selected seat for AVAILABLE seat', () => {
    const seat = {
      seatId: 1,
      rowLabel: 'A',
      seatNumber: 1,
      status: 'AVAILABLE',
    } as Seat;

    spyOn(component.seatSelected, 'emit');

    component.onSeatClick(seat);

    expect(component.seatSelected.emit)
      .toHaveBeenCalledWith(seat);
  });

  it('should not emit seat for BOOKED seat', () => {
    const seat = {
      seatId: 1,
      rowLabel: 'A',
      seatNumber: 1,
      status: 'BOOKED',
    } as Seat;

    spyOn(component.seatSelected, 'emit');

    component.onSeatClick(seat);

    expect(component.seatSelected.emit)
      .not.toHaveBeenCalled();
  });

  it('should sort rows alphabetically', () => {
    const result = component.sortRows(
      {
        key: 'A',
        value: [],
      },
      {
        key: 'B',
        value: [],
      }
    );

    expect(result).toBeLessThan(0);
  });
});