import { Routes } from '@angular/router';
import { CarrerasLista } from './pages/carreras-lista/carreras-lista';
import { MateriasLista } from './pages/materias-lista/materias-lista';
import { PlanesLista } from './pages/planes-lista/planes-lista';

// Las mismas URLs que la API (/api/admin/carreras/...). Los :parámetros llegan a la pantalla
// como input() con el mismo nombre.
const routes: Routes = [
  { path: '', component: CarrerasLista, title: 'Carreras' },
  { path: ':carreraId/planes', component: PlanesLista, title: 'Planes de estudio' },
  {
    path: ':carreraId/planes/:planId/materias',
    component: MateriasLista,
    title: 'Materias del plan',
  },
];

export default routes;
