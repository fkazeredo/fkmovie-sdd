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
  { path: '**', redirectTo: '' },
];
