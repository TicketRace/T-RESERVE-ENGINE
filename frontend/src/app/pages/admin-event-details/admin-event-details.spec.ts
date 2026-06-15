import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminEventDetails } from './admin-event-details';

describe('AdminEventDetails', () => {
  let component: AdminEventDetails;
  let fixture: ComponentFixture<AdminEventDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminEventDetails]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminEventDetails);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
