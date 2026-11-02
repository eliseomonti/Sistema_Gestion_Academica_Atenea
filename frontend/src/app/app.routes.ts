import { Routes } from '@angular/router';
import { rolGuard } from './core/seguridad/rol-guard';

export const routes: Routes = [
  { path: 'login', loadChildren: () => import('./login/login.routes') },
  {
    path: 'admin',
    canMatch: [rolGuard('ADMIN')],
    loadChildren: () => import('./admin/admin.routes'),
  },
  {
    path: 'docente',
    canMatch: [rolGuard('DOCENTE')],
    loadChildren: () => import('./docente/docente.routes'),
  },
  {
    path: 'estudiante',
    canMatch: [rolGuard('ESTUDIANTE')],
    loadChildren: () => import('./estudiante/estudiante.routes'),
  },
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: '**', redirectTo: 'login' },
];
