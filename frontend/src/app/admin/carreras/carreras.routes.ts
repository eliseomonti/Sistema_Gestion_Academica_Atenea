import { Routes } from '@angular/router';
import { CarrerasLista } from './pages/carreras-lista/carreras-lista';
import { PlanesLista } from './pages/planes-lista/planes-lista';

const routes: Routes = [
  { path: '', component: CarrerasLista, title: 'Carreras' },
  { path: ':carreraId/planes', component: PlanesLista, title: 'Planes de estudio' },
];

export default routes;
