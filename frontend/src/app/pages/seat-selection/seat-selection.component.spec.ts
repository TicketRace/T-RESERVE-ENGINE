import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SeatSelectionComponent } from './seat-selection.component';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { HttpClient } from '@angular/common/http';
import { Seat } from '../../models/seat';
import { HttpTestingController } from '@angular/common/http/testing';
import { WebSocketService } from '../../services/websocket.service';

describe('SeatSelectionComponent', () => {
  let component: SeatSelectionComponent;
  let fixture: ComponentFixture<SeatSelectionComponent>;
  let router: jasmine.SpyObj<Router>;
  let httpMock: HttpTestingController;
  let wsSpy: jasmine.SpyObj<any>;

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
    wsSpy = jasmine.createSpyObj('WebSocketService', ['connect', 'disconnect']);

    await TestBed.configureTestingModule({
      imports: [
        SeatSelectionComponent,
        HttpClientTestingModule,
      ],
      providers: [
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
        {
          provide: WebSocketService,
          useValue: wsSpy,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SeatSelectionComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should load event on init', () => {
    fixture.detectChanges();

    const requests = httpMock.match(req =>
      req.method === 'GET'
    );

    const eventReq = requests.find(r =>
      r.request.url.includes('/api/events/1')
    );

    const seatsReq = requests.find(r =>
      r.request.url.includes('/seats')
    );

    expect(eventReq).toBeTruthy();
    expect(seatsReq).toBeTruthy();

    eventReq!.flush({ id: 1, title: 'Concert' });
    seatsReq!.flush([]);

    expect(component.event?.title).toBe('Concert');
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

  //WS callback
  it('should update seats from websocket and regroup them', () => {
    const seats: Seat[] = [
      { seatId: 1, rowLabel: 'A', seatNumber: 2, status: 'AVAILABLE' } as Seat,
      { seatId: 2, rowLabel: 'A', seatNumber: 1, status: 'AVAILABLE' } as Seat,
    ];

    let capturedCallback: ((seats: Seat[]) => void) | undefined;

    wsSpy.connect.and.callFake((eventId: number, cb: any) => {
      expect(eventId).toBe(1);
      capturedCallback = cb;
    });

    fixture.detectChanges();

    expect(wsSpy.connect).toHaveBeenCalled();
    expect(capturedCallback).toBeDefined();

    capturedCallback!(seats);

    expect(component.seats.length).toBe(2);
    expect(component.groupedSeats['A'][0].seatNumber).toBe(1);
  });
});
