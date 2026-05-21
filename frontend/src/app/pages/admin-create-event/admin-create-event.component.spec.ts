import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminCreateEventComponent } from './admin-create-event.component';

import { HttpClientTestingModule, HttpTestingController }
from '@angular/common/http/testing';

import { ActivatedRoute, Router } from '@angular/router';
import { environment } from '../../../environments/environment';

describe('AdminCreateEventComponent', () => {
  let component: AdminCreateEventComponent;
  let fixture: ComponentFixture<AdminCreateEventComponent>;

  let httpMock: HttpTestingController;
  let router: jasmine.SpyObj<Router>;

  const apiUrl = environment.apiUrl; 

  beforeEach(async () => {
    router = jasmine.createSpyObj(
      'Router',
      ['navigate']
    );

    await TestBed.configureTestingModule({
      imports: [
        AdminCreateEventComponent,
        HttpClientTestingModule,
      ],
      providers: [
        {
          provide: Router,
          useValue: router,
        },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => null,
              },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(
      AdminCreateEventComponent
    );

    component = fixture.componentInstance;

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  //create
  it('should create event', () => {
    component.title = 'Concert';
    component.description = 'Music';
    component.selectedVenueId = 1;
    component.startTime = '2026-01-01T18:00';

    component.createEvent();

    const req = httpMock.expectOne(
      `${apiUrl}/api/admin/events`
    );

    expect(req.request.method).toBe('POST');

    expect(req.request.body.title)
      .toBe('Concert');

    req.flush({});
  });

  //validation
  it('should not create event without venue', () => {
    spyOn(window, 'alert');

    component.selectedVenueId = null;

    component.createEvent();

    expect(window.alert)
      .toHaveBeenCalledWith('Выберите площадку');
  });

  //navigation
  it('should navigate to venue builder', () => {
    component.openBuilder();

    expect(router.navigate)
      .toHaveBeenCalledWith(
        ['/admin/venue-builder']
      );
  });

  it('should navigate back to admin', () => {
    component.goBack();

    expect(router.navigate)
      .toHaveBeenCalledWith(['/admin']);
  });
});