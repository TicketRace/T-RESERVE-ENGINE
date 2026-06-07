import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaymentComponent } from './payment.component';
import { ActivatedRoute, Router } from '@angular/router';
import { BookingService } from '../../services/booking.service';
import { of, throwError } from 'rxjs';

describe('PaymentComponent', () => {
  let component: PaymentComponent;
  let fixture: ComponentFixture<PaymentComponent>;

  let bookingService: jasmine.SpyObj<BookingService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    bookingService = jasmine.createSpyObj(
      'BookingService',
      ['lockSeat', 'confirmBooking']
    );

    router = jasmine.createSpyObj(
      'Router',
      ['navigate']
    );

    await TestBed.configureTestingModule({
      imports: [PaymentComponent],
      providers: [
        {
          provide: BookingService,
          useValue: bookingService,
        },
        {
          provide: Router,
          useValue: router,
        },
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
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentComponent);
    component = fixture.componentInstance;
  });

  //pay free
  it('should confirm booking and navigate to success', () => {
    component.lockId = 123;

    bookingService.confirmBooking.and.returnValue(
      of('OK')
    );

    component.payFree();

    expect(
      bookingService.confirmBooking
    ).toHaveBeenCalledWith(123);

    expect(router.navigate)
      .toHaveBeenCalledWith(['/payment-success']);
  });

  it('should set error message on payment error', () => {
    component.lockId = 123;

    bookingService.confirmBooking.and.returnValue(
      throwError(() => new Error())
    );

    component.payFree();

    expect(component.errorMessage)
      .toBe('Ошибка оплаты');
  });

  it('should not confirm booking without lockId', () => {
    component.lockId = null;

    component.payFree();

    expect(
      bookingService.confirmBooking
    ).not.toHaveBeenCalled();
  });

  //go back
  it('should navigate back to event page', () => {
    component.session = {
      id: 5,
    } as any;

    spyOn(window.history, 'back');

    component.goBack();

    expect(window.history.back)
      .toHaveBeenCalled();

    expect(router.navigate).toHaveBeenCalledWith(
      ['/event', 5],
      {
        state: {
          reloadSeats: true,
        },
      }
    );
  });

  //destroy
  it('should clear timer on destroy', () => {
    component['timerInterval'] = setInterval(() => {}, 1000);

    spyOn(window, 'clearInterval');

    component.ngOnDestroy();

    expect(clearInterval).toHaveBeenCalled();
  });

  // ngOnInit
  it('should navigate to events if selectedSeat missing', () => {
    spyOnProperty(window, 'history').and.returnValue({
      state: {},
    } as History);

    component.ngOnInit();

    expect(router.navigate)
      .toHaveBeenCalledWith(['/events']);
  });

  it('should restore lock from sessionStorage', () => {
    const lock = {
      lockId: 99,
      expiresAt: new Date(Date.now() + 60000).toISOString(),
    };

    sessionStorage.setItem(
      'payment_lock',
      JSON.stringify(lock)
    );

    spyOnProperty(window, 'history').and.returnValue({
      state: {
        selectedSeat: {
          seatId: 1,
        },
        session: {
          id: 10,
        },
      },
    } as History);

    spyOn<any>(component, 'startTimer');

    component.ngOnInit();

    expect(component.lockId).toBe(99);

    expect(component['startTimer'])
      .toHaveBeenCalledWith(lock.expiresAt);
  });

  it('should create lock if sessionStorage empty', () => {
    spyOnProperty(window, 'history').and.returnValue({
      state: {
        selectedSeat: {
          seatId: 1,
        },
        session: {
          id: 10,
        },
      },
    } as History);

    spyOn<any>(component, 'createLock');

    component.ngOnInit();

    expect(component['createLock'])
      .toHaveBeenCalledWith(1);
  });

  //create lock
  it('should create lock successfully', () => {
    component.selectedSeat = {
      seatId: 5,
    } as any;

    bookingService.lockSeat.and.returnValue(
      of({
        lockId: 777,
        expiresAt: '2026-01-01T12:00:00',
      })
    );

    spyOn<any>(component, 'startTimer');

    component['createLock'](1);

    expect(component.isLoading).toBeFalse();

    expect(component.lockId).toBe(777);

    expect(sessionStorage.getItem('payment_lock'))
      .toContain('777');

    expect(component['startTimer'])
      .toHaveBeenCalled();
  });

  it('should set occupied message on 409 error', () => {
    jasmine.clock().install();

    component.selectedSeat = {
      seatId: 5,
    } as any;

    bookingService.lockSeat.and.returnValue(
      throwError(() => ({
        status: 409,
      }))
    );

    component['createLock'](1);

    expect(component.errorMessage)
      .toBe('Место уже занято');

    jasmine.clock().tick(2000);

    expect(router.navigate)
      .toHaveBeenCalledWith(['/events']);

    jasmine.clock().uninstall();
  });

  it('should set generic booking error', () => {
    component.selectedSeat = {
      seatId: 5,
    } as any;

    bookingService.lockSeat.and.returnValue(
      throwError(() => ({
        status: 500,
      }))
    );

    component['createLock'](1);

    expect(component.errorMessage)
      .toBe('Ошибка бронирования');
  });

  it('should not create lock without selectedSeat', () => {
    component.selectedSeat = null;

    component['createLock'](1);

    expect(
      bookingService.lockSeat
    ).not.toHaveBeenCalled();
  });

  //timer
  it('should format timer label', () => {
    jasmine.clock().install();

    const future =
      new Date(Date.now() + 65000).toISOString();

    component['startTimer'](future);

    expect(component.expiresLabel)
      .toContain('01:');

    jasmine.clock().uninstall();
  });

  it('should expire timer and navigate away', () => {
    jasmine.clock().install();

    const expired =
      new Date(Date.now() - 1000).toISOString();

    component['startTimer'](expired);

    expect(component.expiresLabel)
      .toBe('00:00');

    expect(router.navigate)
      .toHaveBeenCalledWith(
        ['/events'],
        {
          state: {
            message: 'Время бронирования истекло',
          },
        }
      );

    jasmine.clock().uninstall();
  });
});