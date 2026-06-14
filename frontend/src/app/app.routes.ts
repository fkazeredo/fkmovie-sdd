import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guards';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./features/home/pages/home-page/home-page').then((m) => m.HomePage),
  },
  {
    path: 'sessoes',
    loadComponent: () =>
      import('./features/screenings/pages/screenings-list-page/screenings-list-page').then(
        (m) => m.ScreeningsListPage,
      ),
  },
  {
    path: 'sessoes/:id/assentos',
    loadComponent: () =>
      import('./features/screenings/pages/seat-map-page/seat-map-page').then((m) => m.SeatMapPage),
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/pages/login-page/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'cadastro',
    loadComponent: () =>
      import('./features/auth/pages/register-page/register-page').then((m) => m.RegisterPage),
  },
  {
    // Path dictated by the backend's email link (NotificationEventListener builds `/verify-email`).
    path: 'verify-email',
    loadComponent: () =>
      import('./features/auth/pages/verify-email-page/verify-email-page').then(
        (m) => m.VerifyEmailPage,
      ),
  },
  { path: 'verificar-email', redirectTo: 'verify-email' },
  {
    path: 'reservas/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/reservation/pages/reservation-page/reservation-page').then(
        (m) => m.ReservationPage,
      ),
  },
  {
    path: 'minhas-reservas',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/reservation/pages/my-reservations-page/my-reservations-page').then(
        (m) => m.MyReservationsPage,
      ),
  },
  { path: '**', redirectTo: '' },
];
