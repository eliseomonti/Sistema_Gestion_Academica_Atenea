import { Routes } from '@angular/router';
import { Layout } from './core/layout/layout';
import { rolGuard } from './core/seguridad/rol-guard';

export const routes: Routes = [
  { path: 'login', loadChildren: () => import('./login/login.routes') },
  {
    // Sin path propio: sólo agrega la barra superior alrededor de las rutas de los actores.
    path: '',
    component: Layout,
    children: [
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
    ],
  },
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: '**', redirectTo: 'login' },
];
