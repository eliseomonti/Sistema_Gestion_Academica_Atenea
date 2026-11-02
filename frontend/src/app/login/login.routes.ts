import { Routes } from '@angular/router';
import { Login } from './login';

const routes: Routes = [
  { path: 'admin', component: Login, data: { rol: 'ADMIN' }, title: 'Ingreso administración' },
  { path: 'docente', component: Login, data: { rol: 'DOCENTE' }, title: 'Ingreso docentes' },
  {
    path: 'estudiante',
    component: Login,
    data: { rol: 'ESTUDIANTE' },
    title: 'Ingreso estudiantes',
  },
  { path: '', pathMatch: 'full', redirectTo: 'estudiante' },
];

export default routes;
