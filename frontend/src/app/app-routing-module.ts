import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { EventDetailsComponent } from './pages/event-details/event-details.component';
import { SeatSelectionComponent } from './pages/seat-selection/seat-selection.component';
import { PaymentComponent } from './pages/payment/payment.component';
import { PaymentSuccessComponent } from './pages/payment-success/payment-success.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { AdminComponent } from './pages/admin/admin.component';
import { AdminCreateEventComponent } from './pages/admin-create-event/admin-create-event.component';
import { AdminVenueBuilderComponent } from './pages/admin-venue-builder/admin-venue-builder.component';
import { AuthComponent } from './pages/auth/auth.component';
import { OAuth2CallbackComponent } from './pages/oauth2-callback/oauth2-callback.component';
import { authGuard } from './services/auth.guard';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'event/:id', component: EventDetailsComponent },
  { path: 'event/:id/session/:sessionId/seats', component: SeatSelectionComponent },
  { path: 'payment/:sessionId', component: PaymentComponent, canActivate: [authGuard] },
  { path: 'payment-success', component: PaymentSuccessComponent, canActivate: [authGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
  { path: 'login', component: AuthComponent },
  { path: 'register', component: AuthComponent },
  { path: 'admin', component: AdminComponent, canActivate: [authGuard] },
  { path: 'admin/create', component: AdminCreateEventComponent, canActivate: [authGuard] },
  { path: 'admin/create/:id', component: AdminCreateEventComponent, canActivate: [authGuard] },
  { path: 'oauth2/callback', component: OAuth2CallbackComponent },
  { path: '**', redirectTo: '' }
];