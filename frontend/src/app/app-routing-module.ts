import { Routes } from '@angular/router';
import { EventsComponent } from './pages/events/events.component';
import { EventDetailsComponent } from './pages/event-details/event-details.component';
import { SeatSelectionComponent } from './pages/seat-selection/seat-selection.component';
import { PaymentComponent } from './pages/payment/payment.component';
import { PaymentSuccessComponent } from './pages/payment-success/payment-success.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { AdminComponent } from './pages/admin/admin.component';
import { AdminCreateEventComponent } from './pages/admin-create-event/admin-create-event.component';
import { AdminVenueBuilderComponent } from './pages/admin-venue-builder/admin-venue-builder.component';
import { AuthComponent } from './pages/auth/auth.component';

export const routes: Routes = [
  {path: '', redirectTo: 'login', pathMatch: 'full'},
  { path: 'events', component: EventsComponent },
  { path: 'event/:id', component: EventDetailsComponent },
  { path: 'event/:id/session/:sessionId/seats', component: SeatSelectionComponent },
  { path: 'payment/:sessionId', component: PaymentComponent },
  { path: 'payment-success', component: PaymentSuccessComponent },
  { path: 'profile', component: ProfileComponent },
  { path: 'login', component: AuthComponent },
  { path: 'register', component: AuthComponent },
  { path: 'admin', component: AdminComponent },
  { path: 'admin/create', component: AdminCreateEventComponent },
  //{ path: 'admin/venue-builder', component: AdminVenueBuilderComponent },
  { path: 'admin/create/:id', component: AdminCreateEventComponent },
  { path: '**', redirectTo: 'events' }
];