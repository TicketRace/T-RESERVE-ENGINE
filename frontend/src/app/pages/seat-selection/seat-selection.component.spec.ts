import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SeatSelectionComponent } from './seat-selection.component';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { HttpClient } from '@angular/common/http';
import { Seat } from '../../models/seat';

describe('SeatSelectionComponent', () => {
  let component: SeatSelectionComponent;
  let fixture: ComponentFixture<SeatSelectionComponent>;
  let router: jasmine.SpyObj<Router>;

  const mockSeats: Seat[] = [
    {
      seatId: 1,
      rowLabel: 'B',
      seatNumber: 2,
      status: 'AVAILABLE',
    } as Seat,

    {
      seatId: 2,
      rowLabel: 'A',
      seatNumber: 1,
      status: 'AVAILABLE',
    } as Seat,

    {
      seatId: 3,
      rowLabel: 'B',
      seatNumber: 1,
      status: 'BOOKED',
    } as Seat,
  ];

  beforeEach(async () => {
    router = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [
        SeatSelectionComponent,
        HttpClientTestingModule,
      ],
      providers: [
        HttpClient,
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => '1',
              },
            },
          },
        },
        {
          provide: Router,
          useValue: router,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SeatSelectionComponent);
    component = fixture.componentInstance;
  });

  //selectSeat
  it('should update selectedSeat', () => {
    const seat = mockSeats[0];

    component.selectSeat(seat);

    expect(component.selectedSeat).toEqual(seat);
  });

  //proceed
  it('should navigate to payment page', () => {
    component.event = {
      id: 1,
      title: 'Concert',
    } as any;

    component.selectedSeat = mockSeats[0];

    component.proceed();

    expect(router.navigate).toHaveBeenCalledWith(
      ['/payment', 1],
      {
        state: {
          selectedSeat: mockSeats[0],
          event: component.event,
        },
      }
    );
  });

  it('should not navigate if seat not selected', () => {
    component.selectedSeat = null;

    component.proceed();

    expect(router.navigate).not.toHaveBeenCalled();
  });

  //grouped seats
  it('should group and sort seats by rows and seat number', () => {
    component.seats = mockSeats;

    (component as any).updateGroupedSeats();

    expect(Object.keys(component.groupedSeats))
      .toEqual(['A', 'B']);

    expect(component.groupedSeats['B'][0].seatNumber)
      .toBe(1);

    expect(component.groupedSeats['B'][1].seatNumber)
      .toBe(2);
  });

  //destroy
  it('should complete destroy$ on destroy', () => {
    const nextSpy = spyOn(
      (component as any).destroy$,
      'next'
    );

    const completeSpy = spyOn(
      (component as any).destroy$,
      'complete'
    );

    component.ngOnDestroy();

    expect(nextSpy).toHaveBeenCalled();
    expect(completeSpy).toHaveBeenCalled();
  });
});