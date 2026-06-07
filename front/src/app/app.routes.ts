import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/landing/landing').then((m) => m.Landing),
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./pages/register/register').then((m) => m.Register),
  },
  {
    path: '',
    loadComponent: () =>
      import('./layouts/main-layout/main-layout').then((m) => m.MainLayout),
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'transacoes',
        loadComponent: () =>
          import('./pages/transacoes/transacoes').then((m) => m.Transacoes),
      },
      {
        path: 'extrato',
        loadComponent: () =>
          import('./pages/extrato/extrato').then((m) => m.Extrato),
      },
      {
        path: 'cartoes',
        loadComponent: () =>
          import('./pages/cartoes/cartoes').then((m) => m.Cartoes),
      },
      {
        path: 'faturas',
        loadComponent: () =>
          import('./pages/faturas/faturas').then((m) => m.Faturas),
      },
    ],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
