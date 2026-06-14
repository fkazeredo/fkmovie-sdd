import { Routes } from '@angular/router';

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
    path: 'verificar-email',
    loadComponent: () =>
      import('./features/auth/pages/verify-email-page/verify-email-page').then(
        (m) => m.VerifyEmailPage,
      ),
  },
  { path: '**', redirectTo: '' },
];
